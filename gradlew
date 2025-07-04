#!/usr/bin/env sh

#
# Copyright 2011-2021 the original author or authors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

##############################################################################
##
##  Gradle start up script for UN*X
##
##############################################################################

# Determine the Java command to use to start the JVM.
if [ -n "" ] ; then
    JAVA_HOME=""
fi
if [ -n "$JAVA_HOME" ] ; then
    if [ -x "$JAVA_HOME/jre/sh/java" ] ; then
        # IBM's JDK on AIX uses strange locations for the executables
        JAVACMD="$JAVA_HOME/jre/sh/java"
    else
        JAVACMD="$JAVA_HOME/bin/java"
    fi
    if [ ! -x "$JAVACMD" ] ; then
        die "ERROR: JAVA_HOME is set to an invalid directory: $JAVA_HOME\n\nPlease set the JAVA_HOME environment variable in your shell to the correct location."
    fi
else
    JAVACMD="java"
fi

# Determine the script directory.
SCRIPT_DIR=$(dirname "$0")

# Add default JVM options here. You can also use JAVA_OPTS and GRADLE_OPTS to pass JVM options to this script.
DEFAULT_JVM_OPTS="-Xmx64m -Xms64m"

# Find the Gradle distribution.
GRADLE_HOME="$SCRIPT_DIR"

# Set the Gradle wrapper JAR.
GRADLE_WRAPPER_JAR="$GRADLE_HOME/gradle/wrapper/gradle-wrapper.jar"

# Set the Gradle wrapper properties.
GRADLE_WRAPPER_PROPERTIES="$GRADLE_HOME/gradle/wrapper/gradle-wrapper.properties"

# Set the Gradle wrapper distribution URL.
GRADLE_DISTRIBUTION_URL=$(grep "^distributionUrl" "$GRADLE_WRAPPER_PROPERTIES" | cut -d= -f2)

# Set the Gradle wrapper distribution path.
GRADLE_DISTRIBUTION_PATH="$GRADLE_HOME/gradle/wrapper/dists"

# Set the Gradle wrapper distribution name.
GRADLE_DISTRIBUTION_NAME=$(basename "$GRADLE_DISTRIBUTION_URL")

# Set the Gradle wrapper distribution file.
GRADLE_DISTRIBUTION_FILE="$GRADLE_DISTRIBUTION_PATH/$GRADLE_DISTRIBUTION_NAME"

# Download the Gradle distribution if it doesn't exist.
if [ ! -f "$GRADLE_DISTRIBUTION_FILE" ] ; then
    echo "Downloading Gradle distribution from $GRADLE_DISTRIBUTION_URL"
    mkdir -p "$GRADLE_DISTRIBUTION_PATH"
    curl -L "$GRADLE_DISTRIBUTION_URL" -o "$GRADLE_DISTRIBUTION_FILE"
fi

# Run Gradle.
exec "$JAVACMD" $DEFAULT_JVM_OPTS $JAVA_OPTS $GRADLE_OPTS -jar "$GRADLE_WRAPPER_JAR" "$@"


