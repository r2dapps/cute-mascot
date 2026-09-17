@echo off
:: Build script for CuteMascot APK
:: Redirects Gradle cache entirely to D: drive -- does NOT touch C:\Users

SET JAVA_HOME=C:\Program Files\Android\Android Studio\jre
SET ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk
SET GRADLE_USER_HOME=D:\Desktop-apps\Desktop-Mascot\android\.gradle-cache

ECHO ==== Building CuteMascot APK ====
ECHO Java:   %JAVA_HOME%
ECHO SDK:    %ANDROID_HOME%
ECHO Cache:  %GRADLE_USER_HOME%
ECHO.

gradlew.bat assembleDebug

IF %ERRORLEVEL% EQU 0 (
    ECHO.
    ECHO ==== SUCCESS ====
    ECHO APK: android\app\build\outputs\apk\debug\app-debug.apk
    ECHO.
    ECHO To install on connected phone:
    ECHO   %ANDROID_HOME%\platform-tools\adb install -r app\build\outputs\apk\debug\app-debug.apk
) ELSE (
    ECHO ==== BUILD FAILED - Check errors above ====
)
