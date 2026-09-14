from pathlib import Path

main = Path('app/src/main/java/com/wavelength/music/MainActivity.kt')
s = main.read_text()

# Add lifecycleScope import for foreground polling.
old = 'import androidx.lifecycle.compose.collectAsStateWithLifecycle\n'
new = 'import androidx.lifecycle.compose.collectAsStateWithLifecycle\nimport androidx.lifecycle.lifecycleScope\n'
if new not in s:
    if old not in s: raise SystemExit('lifecycle import anchor missing')
    s = s.replace(old, new, 1)

old = 'import kotlinx.coroutines.Dispatchers\nimport kotlinx.coroutines.withContext\n'
new = 'import kotlinx.coroutines.Dispatchers\nimport kotlinx.coroutines.delay\nimport kotlinx.coroutines.launch\nimport kotlinx.coroutines.withContext\n'
if new not in s:
    if old not in s: raise SystemExit('coroutine import anchor missing')
    s = s.replace(old, new, 1)

# Start a foreground watcher immediately after enqueueing the exact download.
old = '''        updaterPrefs.edit().putLong(KEY_PENDING_DOWNLOAD_ID, downloadId).apply()\n        Toast.makeText(this, "AzMusic update downloading…", Toast.LENGTH_SHORT).show()\n\n        val receiver = object : BroadcastReceiver() {'''
new = '''        updaterPrefs.edit().putLong(KEY_PENDING_DOWNLOAD_ID, downloadId).apply()\n        Toast.makeText(this, "AzMusic update downloading…", Toast.LENGTH_SHORT).show()\n\n        // Keep a foreground watcher as the primary path. Some Android builds deliver the\n        // DownloadManager completion broadcast late, or won't allow the receiver to launch an\n        // activity immediately. While AzMusic is visible, poll this exact download and open the\n        // installer the moment it becomes successful. The broadcast receiver and onResume()\n        // logic below remain as fallbacks.\n        watchDownloadAndLaunchInstaller(downloadId)\n\n        val receiver = object : BroadcastReceiver() {'''
if new not in s:
    if old not in s: raise SystemExit('download enqueue anchor missing')
    s = s.replace(old, new, 1)

# Add watcher method before query/launch helper.
anchor = '    private fun tryLaunchDownloadedUpdate(downloadId: Long): Boolean {\n'
method = '''    private fun watchDownloadAndLaunchInstaller(downloadId: Long) {\n        lifecycleScope.launch {\n            // Stay lightweight: DownloadManager status changes are slow compared with UI frames.\n            while (!isFinishing && updaterPrefs.getLong(KEY_PENDING_DOWNLOAD_ID, -1L) == downloadId) {\n                if (tryLaunchDownloadedUpdate(downloadId)) return@launch\n                delay(500L)\n            }\n        }\n    }\n\n'''
if method not in s:
    if anchor not in s: raise SystemExit('installer helper anchor missing')
    s = s.replace(anchor, method + anchor, 1)

# Clarify unknown-source flow: it continues automatically on return.
s = s.replace(
    '"Allow AzMusic to install updates, then return and tap Download & install again.",',
    '"Allow AzMusic to install updates. When you return, the update will continue automatically.",'
)
main.write_text(s)

about = Path('app/src/main/java/com/wavelength/music/ui/settings/AboutSheet.kt')
a = about.read_text()
entry = '    "Updater now opens the Android installer automatically as soon as the APK download finishes",\n'
marker = 'private val latestUpdates = listOf(\n'
if entry not in a:
    if marker not in a: raise SystemExit('latestUpdates anchor missing')
    a = a.replace(marker, marker + entry, 1)
about.write_text(a)
