@echo off
cd /d D:\Temp\REPassCard
set JAVA_OPTS=-Dorg.gradle.native=false
set GRADLE_OPTS=-Dorg.gradle.native=false
java -Dorg.gradle.native=false -cp gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain assembleDebug --no-daemon
