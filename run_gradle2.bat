@echo off
cd /d D:\Temp\REPassCard
set JAVA_OPTS=-Dorg.gradle.native=false
"C:\Users\Administrator\.gradle\wrapper\dists\gradle-8.9-bin\90cnw93cvbtalezasaz0blq0a\gradle-8.9\bin\gradle.bat" assembleDebug --no-daemon -Dorg.gradle.native=false
