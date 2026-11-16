@rem
@rem Gradle wrapper start script for Windows (minimal version).
@rem Runs the official Gradle wrapper (gradle\wrapper\gradle-wrapper.jar, gradle-8.10.2-wrapper.jar), which downloads and
@rem verifies the Gradle distribution named in gradle-wrapper.properties.
@rem Regenerate the full upstream scripts at any time with: gradlew wrapper --gradle-version 8.10.2
@rem
@if "%DEBUG%"=="" @echo off
setlocal

set APP_HOME=%~dp0
set CLASSPATH=%APP_HOME%gradle\wrapper\gradle-wrapper.jar

if defined JAVA_HOME (
    set JAVA_EXE=%JAVA_HOME%\bin\java.exe
) else (
    set JAVA_EXE=java.exe
)

"%JAVA_EXE%" %JAVA_OPTS% %GRADLE_OPTS% "-Dorg.gradle.appname=gradlew" -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*
exit /b %ERRORLEVEL%
