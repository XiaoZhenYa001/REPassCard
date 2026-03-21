$env:JAVA_HOME = "C:\Program Files\java\jdk-17"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
$env:GRADLE_USER_HOME = "$env:LOCALAPPDATA\GradleCache"

# Create the wrapper directory
$wrapperDir = "$env:GRADLE_USER_HOME\wrapper\dists\gradle-8.9-bin\90cnw93cvbtalezasaz0blq0a"
Write-Host "Creating directory: $wrapperDir"
New-Item -ItemType Directory -Path $wrapperDir -Force -ErrorAction Continue | Out-Null

Write-Host "Using JDK 17: $(& java -version 2>&1 | Select-Object -First 1)"
Write-Host "Running Gradle..."

cd D:\Temp\REPassCard
& ".\gradlew.bat" assembleDebug --no-daemon
