const JSON_HEADERS = { 'content-type': 'application/json; charset=utf-8' };
const ALLOWED_DURATIONS = new Set([30, 60, 90]);
const DAY_MS = 24 * 60 * 60 * 1000;
const PERSISTENT_SESSION_EXPIRES_AT = Number.MAX_SAFE_INTEGER;
const REQUEST_COOLDOWN_MS = 60 * 1000;
let deviceSchemaReadyPromise;
let usageSchemaReadyPromise;

export default {
  async fetch(request, env) {
    try {
      const url = new URL(request.url);
      await ensureDeviceBindingSchema(env);
      await ensureUsageStatsSchema(env);
      if (request.method === 'OPTIONS') return new Response(null, { status: 204 });

      if (request.method === 'POST' && url.pathname === '/activation/request') {
        return requestActivation(request, env);
      }
      if (request.method === 'GET' && url.pathname === '/activation/status') {
        return activationStatus(url, env);
      }
      if (request.method === 'POST' && url.pathname === '/admin/login') {
        return adminLogin(request, env);
      }
      if (request.method === 'POST' && url.pathname === '/usage/report') {
        return reportUsage(request, env);
      }

      const adminMatch = url.pathname.match(/^\/admin\/requests\/(\d+)\/(approve|reject|revoke|reset-device)$/);
      if (request.method === 'GET' && url.pathname === '/admin/requests') {
        const auth = await requireAdmin(request, env);
        if (!auth.ok) return auth.response;
        return listRequests(env);
      }
      if (request.method === 'GET' && url.pathname === '/admin/user-stats') {
        const auth = await requireAdmin(request, env);
        if (!auth.ok) return auth.response;
        return listUserStats(env);
      }
      if (request.method === 'POST' && adminMatch) {
        const auth = await requireAdmin(request, env);
        if (!auth.ok) return auth.response;
        return decideRequest(request, env, Number(adminMatch[1]), adminMatch[2]);
      }

      return json({ error: 'Not found' }, 404);
    } catch (error) {
      console.error(error);
      return json({ error: 'Server unavailable' }, 500);
    }
  }
};

async function requestActivation(request, env) {
  const body = await readJson(request);
  const email = normalizeEmail(body?.email);
  const deviceId = normalizeDeviceId(body?.deviceId);
  if (!isValidEmail(email)) return json({ error: 'Invalid email' }, 400);
  if (!isValidDeviceId(deviceId)) return json({ error: 'Invalid device identifier' }, 400);

  const now = Date.now();
  const existing = await env.DB.prepare(
    'SELECT * FROM activations WHERE email = ? LIMIT 1'
  ).bind(email).first();

  if (existing?.device_id && existing.device_id !== deviceId) {
    return json({ error: 'This email is already linked to another device.', code: 'DEVICE_MISMATCH' }, 409);
  }

  if (existing && !existing.device_id) {
    await env.DB.prepare('UPDATE activations SET device_id=?, updated_at=? WHERE id=? AND device_id IS NULL')
      .bind(deviceId, now, existing.id).run();
    existing.device_id = deviceId;
  }

  if (existing?.requested_at && now - existing.requested_at < REQUEST_COOLDOWN_MS) {
    return json({ error: 'Please wait before submitting another request.' }, 429);
  }

  if (existing?.status === 'ACTIVE' && (existing.expiration_date == null || existing.expiration_date > now)) {
    return json({ activation: toRecord(existing), serverTimestamp: now });
  }

  await env.DB.prepare(`
    INSERT INTO activations(email, status, requested_at, approved_at, activation_start_date, expiration_date, duration_days, device_id, updated_at)
    VALUES(?, 'PENDING', ?, NULL, NULL, NULL, NULL, ?, ?)
    ON CONFLICT(email) DO UPDATE SET
      status='PENDING', requested_at=excluded.requested_at, approved_at=NULL,
      activation_start_date=NULL, expiration_date=NULL, duration_days=NULL, updated_at=excluded.updated_at
  `).bind(email, now, deviceId, now).run();

  const row = await env.DB.prepare('SELECT * FROM activations WHERE email = ? LIMIT 1').bind(email).first();
  return json({ activation: toRecord(row), serverTimestamp: now }, 200);
}

async function activationStatus(url, env) {
  const email = normalizeEmail(url.searchParams.get('email'));
  const deviceId = normalizeDeviceId(url.searchParams.get('deviceId'));
  if (!isValidEmail(email)) return json({ error: 'Invalid email' }, 400);
  if (!isValidDeviceId(deviceId)) return json({ error: 'Invalid device identifier' }, 400);

  const now = Date.now();
  let row = await env.DB.prepare('SELECT * FROM activations WHERE email = ? LIMIT 1').bind(email).first();
  if (!row) {
    return json({ activation: emptyRecord(email), serverTimestamp: now });
  }

  if (row.device_id && row.device_id !== deviceId) {
    return json({ error: 'This email is already linked to another device.', code: 'DEVICE_MISMATCH' }, 409);
  }
  if (!row.device_id) {
    await env.DB.prepare('UPDATE activations SET device_id=?, updated_at=? WHERE id=? AND device_id IS NULL')
      .bind(deviceId, now, row.id).run();
    row = { ...row, device_id: deviceId, updated_at: now };
  }

  if (row.status === 'ACTIVE' && row.expiration_date && row.expiration_date <= now) {
    await env.DB.prepare("UPDATE activations SET status='EXPIRED', updated_at=? WHERE id=?")
      .bind(now, row.id).run();
    row = { ...row, status: 'EXPIRED', updated_at: now };
  }
  return json({ activation: toRecord(row), serverTimestamp: now });
}

async function reportUsage(request, env) {
  const body = await readJson(request);
  const email = normalizeEmail(body?.email);
  const deviceId = normalizeDeviceId(body?.deviceId);
  const playCountDelta = Math.max(0, Math.min(100, Number(body?.playCountDelta || 0)));
  const listenedMsDelta = Math.max(0, Math.min(60 * 60 * 1000, Number(body?.listenedMsDelta || 0)));

  if (!isValidEmail(email)) return json({ error: 'Invalid email' }, 400);
  if (!isValidDeviceId(deviceId)) return json({ error: 'Invalid device identifier' }, 400);
  if (!Number.isFinite(playCountDelta) || !Number.isFinite(listenedMsDelta)) {
    return json({ error: 'Invalid usage values' }, 400);
  }

  const now = Date.now();
  const activation = await env.DB.prepare(
    'SELECT status, expiration_date, device_id FROM activations WHERE email=? LIMIT 1'
  ).bind(email).first();

  if (!activation) return json({ error: 'Activation not found' }, 404);
  if (activation.device_id && activation.device_id !== deviceId) {
    return json({ error: 'Device mismatch' }, 409);
  }
  if (activation.status !== 'ACTIVE' ||
      (activation.expiration_date != null && activation.expiration_date <= now)) {
    return json({ error: 'Activation is not active' }, 403);
  }

  await env.DB.prepare(`
    INSERT INTO user_stats(email, total_plays, total_listened_ms, updated_at)
    VALUES(?, ?, ?, ?)
    ON CONFLICT(email) DO UPDATE SET
      total_plays = total_plays + excluded.total_plays,
      total_listened_ms = total_listened_ms + excluded.total_listened_ms,
      updated_at = excluded.updated_at
  `).bind(email, Math.trunc(playCountDelta), Math.trunc(listenedMsDelta), now).run();

  return json({ ok: true, serverTimestamp: now });
}

async function listUserStats(env) {
  const now = Date.now();
  const result = await env.DB.prepare(
    'SELECT email, total_plays, total_listened_ms, updated_at FROM user_stats ORDER BY total_listened_ms DESC, total_plays DESC'
  ).all();
  return json({
    users: (result.results || []).map(row => ({
      email: row.email,
      totalPlays: Number(row.total_plays || 0),
      totalListenedMs: Number(row.total_listened_ms || 0),
      updatedAt: Number(row.updated_at || 0)
    })),
    serverTimestamp: now
  });
}

async function adminLogin(request, env) {
  const body = await readJson(request);
  const email = normalizeEmail(body?.email);
  const password = String(body?.password || '');
  if (!env.ADMIN_EMAIL || !env.ADMIN_PASSWORD || !env.SESSION_SECRET) {
    return json({ error: 'Admin authentication is not configured' }, 503);
  }

  const validEmail = timingSafeEqual(email, normalizeEmail(env.ADMIN_EMAIL));
  const validPassword = timingSafeEqual(password, env.ADMIN_PASSWORD);
  if (!validEmail || !validPassword) return json({ error: 'Invalid credentials' }, 401);

  // This signed token remains valid until the app explicitly removes it on logout, or the
  // SESSION_SECRET is rotated on Cloudflare. The admin password itself is never stored in the app.
  const expiresAt = PERSISTENT_SESSION_EXPIRES_AT;
  const payload = `${email}|${expiresAt}`;
  const signature = await sign(payload, env.SESSION_SECRET);
  return json({ token: `${base64Url(payload)}.${signature}`, expiresAt });
}

async function listRequests(env) {
  const now = Date.now();
  await env.DB.prepare("UPDATE activations SET status='EXPIRED', updated_at=? WHERE status='ACTIVE' AND expiration_date <= ?")
    .bind(now, now).run();
  const result = await env.DB.prepare('SELECT * FROM activations ORDER BY requested_at DESC, id DESC').all();
  return json({ requests: (result.results || []).map(toRecord), serverTimestamp: now });
}

async function decideRequest(request, env, id, action) {
  const now = Date.now();
  const row = await env.DB.prepare('SELECT * FROM activations WHERE id=? LIMIT 1').bind(id).first();
  if (!row) return json({ error: 'Activation request not found' }, 404);

  if (action === 'approve') {
    const body = await readJson(request);
    const requestedDuration = body?.durationDays;
    const isLifetime = body?.lifetime === true;
    const durationDays = isLifetime ? null : Number(requestedDuration);
    if (!isLifetime && !ALLOWED_DURATIONS.has(durationDays)) {
      return json({ error: 'Invalid activation duration' }, 400);
    }
    const expiration = isLifetime ? null : now + durationDays * DAY_MS;
    await env.DB.prepare(`
      UPDATE activations SET status='ACTIVE', approved_at=?, activation_start_date=?, expiration_date=?, duration_days=?, updated_at=? WHERE id=?
    `).bind(now, now, expiration, durationDays, now, id).run();
  } else if (action === 'reject') {
    await env.DB.prepare("UPDATE activations SET status='REJECTED', updated_at=? WHERE id=?")
      .bind(now, id).run();
  } else if (action === 'revoke') {
    await env.DB.prepare("UPDATE activations SET status='REVOKED', updated_at=? WHERE id=?")
      .bind(now, id).run();
  } else if (action === 'reset-device') {
    await env.DB.prepare('UPDATE activations SET device_id=NULL, updated_at=? WHERE id=?')
      .bind(now, id).run();
  }

  const updated = await env.DB.prepare('SELECT * FROM activations WHERE id=? LIMIT 1').bind(id).first();
  return json({ activation: toRecord(updated), serverTimestamp: now });
}

async function requireAdmin(request, env) {
  const header = request.headers.get('authorization') || '';
  const token = header.startsWith('Bearer ') ? header.slice(7) : '';
  const verified = await verifyToken(token, env.SESSION_SECRET || '');
  if (!verified) return { ok: false, response: json({ error: 'Unauthorized' }, 401) };
  return { ok: true };
}

async function verifyToken(token, secret) {
  if (!token || !secret) return false;
  const parts = token.split('.');
  if (parts.length !== 2) return false;
  let payload;
  try { payload = base64UrlDecode(parts[0]); } catch { return false; }
  const expected = await sign(payload, secret);
  if (!timingSafeEqual(parts[1], expected)) return false;
  const split = payload.lastIndexOf('|');
  if (split < 0) return false;
  const expiresAt = Number(payload.slice(split + 1));
  return Number.isFinite(expiresAt);
}

async function sign(payload, secret) {
  const key = await crypto.subtle.importKey(
    'raw', new TextEncoder().encode(secret),
    { name: 'HMAC', hash: 'SHA-256' }, false, ['sign']
  );
  const sig = await crypto.subtle.sign('HMAC', key, new TextEncoder().encode(payload));
  return base64UrlBytes(new Uint8Array(sig));
}

function toRecord(row) {
  return {
    id: row.id,
    email: row.email,
    status: row.status,
    requestedAt: row.requested_at ?? null,
    approvedAt: row.approved_at ?? null,
    activationStartDate: row.activation_start_date ?? null,
    expirationDate: row.expiration_date ?? null,
    durationDays: row.duration_days ?? null,
    deviceBound: Boolean(row.device_id)
  };
}

function emptyRecord(email) {
  return {
    id: null, email, status: 'NOT_ACTIVATED', requestedAt: null,
    approvedAt: null, activationStartDate: null, expirationDate: null, durationDays: null,
    deviceBound: false
  };
}

async function ensureDeviceBindingSchema(env) {
  if (!deviceSchemaReadyPromise) {
    deviceSchemaReadyPromise = env.DB.prepare('ALTER TABLE activations ADD COLUMN device_id TEXT').run()
      .catch(error => {
        const message = String(error?.message || error || '');
        if (!message.toLowerCase().includes('duplicate column')) throw error;
      });
  }
  return deviceSchemaReadyPromise;
}

async function ensureUsageStatsSchema(env) {
  if (!usageSchemaReadyPromise) {
    usageSchemaReadyPromise = env.DB.prepare(`
      CREATE TABLE IF NOT EXISTS user_stats (
        email TEXT PRIMARY KEY,
        total_plays INTEGER NOT NULL DEFAULT 0,
        total_listened_ms INTEGER NOT NULL DEFAULT 0,
        updated_at INTEGER NOT NULL
      )
    `).run();
  }
  return usageSchemaReadyPromise;
}

function normalizeDeviceId(value) { return String(value || '').trim(); }
function isValidDeviceId(value) { return typeof value === 'string' && value.length >= 8 && value.length <= 128 && /^[A-Za-z0-9._:-]+$/.test(value); }
function normalizeEmail(value) { return String(value || '').trim().toLowerCase(); }
function isValidEmail(value) { return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value) && value.length <= 254; }
function timingSafeEqual(a, b) {
  a = String(a); b = String(b);
  let diff = a.length ^ b.length;
  const length = Math.max(a.length, b.length);
  for (let i = 0; i < length; i++) diff |= (a.charCodeAt(i) || 0) ^ (b.charCodeAt(i) || 0);
  return diff === 0;
}
function base64Url(text) { return base64UrlBytes(new TextEncoder().encode(text)); }
function base64UrlBytes(bytes) {
  let binary = '';
  for (const byte of bytes) binary += String.fromCharCode(byte);
  return btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/g, '');
}
function base64UrlDecode(value) {
  const base64 = value.replace(/-/g, '+').replace(/_/g, '/').padEnd(Math.ceil(value.length / 4) * 4, '=');
  const binary = atob(base64);
  const bytes = Uint8Array.from(binary, c => c.charCodeAt(0));
  return new TextDecoder().decode(bytes);
}
async function readJson(request) {
  try { return await request.json(); } catch { return null; }
}
function json(body, status = 200) {
  return new Response(JSON.stringify(body), { status, headers: JSON_HEADERS });
}
