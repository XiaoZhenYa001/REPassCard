# Delete the lock file
$lockFile = "C:\Users\Administrator\.gradle\wrapper\dists\gradle-8.9-bin\90cnw93cvbtalezasaz0blq0a\gradle-8.9-bin.zip.lck"

# Try multiple times quickly
for ($i = 0; $i -lt 10; $i++) {
    if (Test-Path $lockFile) {
        Remove-Item $lockFile -Force -ErrorAction SilentlyContinue
    }
    Start-Sleep -Milliseconds 100
}

# Now run gradle
Set-Location "D:\Temp\REPassCard"
& ".\gradlew.bat" assembleDebug --no-daemon
