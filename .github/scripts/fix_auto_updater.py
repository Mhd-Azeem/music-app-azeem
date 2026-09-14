from pathlib import Path

# Make release metadata machine-readable by the in-app updater.
w=Path('.github/workflows/build-apk.yml')
s=w.read_text()
old='''          body: |
            Automatically rebuilt from commit ${{ github.sha }}.

            `AzMusic.apk` is the normal app.'''
new='''          body: |
            Build number: ${{ github.run_number }}
            Automatically rebuilt from commit ${{ github.sha }}.

            `AzMusic.apk` is the normal app.'''
if old not in s:
    raise SystemExit('release body block not found')
w.write_text(s.replace(old,new,1))

# Never let the updater accidentally select the Sinhala demo APK.
p=Path('app/src/main/java/com/wavelength/music/MainActivity.kt')
s=p.read_text()
old='''                    val name = asset.optString("name")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        apkUrl = asset.optString("browser_download_url")
                        if (apkUrl.isNotBlank()) break
                    }'''
new='''                    val name = asset.optString("name")
                    if (name.equals("AzMusic.apk", ignoreCase = true)) {
                        apkUrl = asset.optString("browser_download_url")
                        if (apkUrl.isNotBlank()) break
                    }'''
if old not in s:
    raise SystemExit('APK asset selection block not found')
p.write_text(s.replace(old,new,1))

# Record the repair in About.
a=Path('app/src/main/java/com/wavelength/music/ui/settings/AboutSheet.kt')
t=a.read_text()
needle='private val latestUpdates = listOf(\n'
entry='    "Fixed auto updater detection by restoring release build metadata and selecting only the production AzMusic APK",\n'
if entry not in t:
    t=t.replace(needle,needle+entry,1)
a.write_text(t)
