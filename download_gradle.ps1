$ErrorActionPreference = "Continue"
$url = "https://services.gradle.org/distributions/gradle-8.7-bin.zip"
$dest = "C:\Users\Administrator\gradle-8.7.zip"

# Try using WebClient
Write-Host "Downloading Gradle 8.7 using WebClient..."
$webClient = New-Object System.Net.WebClient
$webClient.DownloadFile($url, $dest)

Write-Host "Download complete!"

# Extract
Write-Host "Extracting..."
Expand-Archive -Path $dest -DestinationPath "C:\Users\Administrator" -Force

Write-Host "Running Gradle..."
cd C:\Temp\REPassCard
& "C:\Users\Administrator\gradle-8.7\bin\gradle.bat" assembleDebug --no-daemon
