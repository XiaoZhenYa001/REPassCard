# Stop all QClaw processes
Get-Process | Where-Object { $_.Name -like "*QClaw*" } | Stop-Process -Force -ErrorAction SilentlyContinue
Start-Sleep -Seconds 3

# Clean gradle cache
$gradleDir = "C:\Users\Administrator\.gradle\wrapper\dists\gradle-8.9-bin\90cnw93cvbtalezasaz0blq0a"
if (Test-Path $gradleDir) {
    Get-ChildItem $gradleDir | Remove-Item -Recurse -Force -ErrorAction SilentlyContinue
}
Write-Host "QClaw stopped and gradle cleaned"
