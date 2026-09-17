@echo off
SET JAVA_HOME=C:\Program Files\Android\Android Studio\jre
SET ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk
SET GRADLE_USER_HOME=D:\Desktop-apps\Desktop-Mascot\android\.gradle-cache
"%JAVA_HOME%\bin\java.exe" -classpath "%~dp0gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain %*
