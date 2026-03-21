# Try to delete the lock file using PowerShell with admin privileges
$path = "C:\Users\Administrator\.gradle\wrapper\dists\gradle-8.9-bin\90cnw93cvbtalezasaz0blq0a\gradle-8.9-bin.zip.lck"

# First try: Set file to not read-only and delete
try {
    $file = Get-Item $path -Force
    $file.Attributes = $file.Attributes -band (-bnot [System.IO.FileAttributes]::ReadOnly)
    Remove-Item $path -Force
    Write-Host "Deleted successfully"
} catch {
    Write-Host "First method failed: $($_.Exception.Message)"
    
    # Second try: Use [System.IO.File]::Delete directly
    try {
        [System.IO.File]::Delete($path)
        Write-Host "Deleted with System.IO.File"
    } catch {
        Write-Host "Second method failed: $($_.Exception.Message)"
    }
}
