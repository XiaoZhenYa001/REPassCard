# Download fresh Gradle 8.7 to D drive
$url = "https://services.gradle.org/distributions/gradle-8.7-bin.zip"
$dest = "D:\gradle-8.7.zip"

Write-Host "Downloading Gradle 8.7..."
Invoke-WebRequest -Uri $url -OutFile $dest -UseBasicParsing

Write-Host "Extracting..."
Expand-Archive -Path $dest -DestinationPath "D:\" -Force

Write-Host "Done. Running build..."
Set-Location D:\Temp\REPassCard
& "D:\gradle-8.7\bin\gradle.bat" assembleDebug --no-daemon
