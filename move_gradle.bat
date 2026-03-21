@echo off
echo Attempting to move the entire gradle directory...
move "C:\Users\Administrator\.gradle\wrapper\dists\gradle-8.9-bin" "C:\Users\Administrator\.gradle\wrapper\dists\gradle-8.9-bin-old"
if errorlevel 1 (
    echo Move failed, trying xcopy and rmdir...
    xcopy /E /I "C:\Users\Administrator\.gradle\wrapper\dists\gradle-8.9-bin" "C:\Users\Administrator\.gradle\wrapper\dists\gradle-8.9-bin-backup"
    rmdir /S /Q "C:\Users\Administrator\.gradle\wrapper\dists\gradle-8.9-bin"
)
echo Done.
