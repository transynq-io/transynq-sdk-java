#!/bin/sh
#
# Gradle wrapper start script (minimal POSIX version).
# Runs the official Gradle wrapper (gradle/wrapper/gradle-wrapper.jar, SHA-256
# 2db75c40782f5e8ba1fc278a5574bab070adccb2d21ca5a6e5ed840888448046 = gradle-8.10.2-wrapper.jar),
# which downloads and verifies the Gradle distribution named in gradle-wrapper.properties.
# Regenerate the full upstream scripts at any time with: ./gradlew wrapper --gradle-version 8.10.2
#
set -e

APP_HOME=$(cd "$(dirname "$0")" && pwd -P)
CLASSPATH="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"

if [ -n "$JAVA_HOME" ]; then
    JAVACMD="$JAVA_HOME/bin/java"
    [ -x "$JAVACMD" ] || { echo "ERROR: JAVA_HOME is set to an invalid directory: $JAVA_HOME" >&2; exit 1; }
else
    JAVACMD=java
    command -v java >/dev/null 2>&1 || { echo "ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH." >&2; exit 1; }
fi

exec "$JAVACMD" $JAVA_OPTS $GRADLE_OPTS "-Dorg.gradle.appname=gradlew" -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
