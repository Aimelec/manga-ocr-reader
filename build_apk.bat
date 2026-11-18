@echo off
rem Rebuilds the Android app and copies it to MangaReader.apk (download on the phone: https://<PC IP>:8765/app.apk)
cd /d "%~dp0android"
rem Uses your JAVA_HOME if it is set, otherwise the default JDK 21 location.
if not defined JAVA_HOME set "JAVA_HOME=C:\Program Files\Java\jdk-21"
set "GRADLE_USER_HOME=%~dp0tools\gradle-home"
call "%~dp0tools\gradle\gradle-9.8.1\bin\gradle.bat" assembleDebug --console=plain
if errorlevel 1 goto end
copy /y "app\build\outputs\apk\debug\app-debug.apk" "%~dp0MangaReader.apk"
echo.
echo Done: MangaReader.apk
:end
pause
