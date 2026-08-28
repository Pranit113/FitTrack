#!/usr/bin/env sh
#
# Gradle start up script for UN*X
#
DIRNAME=`dirname "$0"`
APP_BASE_NAME=`basename "$0"`
APP_HOME=`cd "$DIRNAME" >/dev/null; pwd`
MAX_FD="maximum"
warn () {
    echo "$*"
}
die () {
    echo
    echo "$*"
    echo
    exit 1
}
case "`uname`" in
  CYGWIN* )
    cygwin=true
    ;;
  Darwin* )
    darwin=true
    ;;
  MINGW* )
    msys=true
    ;;
esac
CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar
JAVACMD=java
if command -v java > /dev/null 2>&1; then
    JAVACMD=`which java`
fi
exec "$JAVACMD" -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
