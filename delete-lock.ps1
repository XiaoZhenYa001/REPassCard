# Kill any processes holding the lock
Get-Process | Where-Object { $_.Path -like "*gradle*" -or $_.Name -eq "java" } | Stop-Process -Force -ErrorAction SilentlyContinue
Start-Sleep -Seconds 3

# Try to delete the lock file
$lockPath = "C:\Users\Administrator\.gradle\wrapper\dists\gradle-8.9-bin\90cnw93cvbtalezasaz0blq0a\gradle-8.9-bin.zip.lck"
if (Test-Path $lockPath) {
    Remove-Item $lockPath -Force -ErrorAction SilentlyContinue
    Write-Host "Deleted lock file"
} else {
    Write-Host "Lock file not found"
}
