#!/usr/bin/env sh

# This script is a launcher that compensates for the script code produced by sbt-native-packager, which
# is not ash-compatible (busybox uses ash, not bash).

if [ "$1" = "sh" ]; then
	sh
else
	sh "$(sed -E 's/app_mainclass=\(\"(.*)\"\)/app_mainclass=\"\1\"/' $1) $@"
#	sed -E 's/app_mainclass=\(\"(.*)\"\)/app_mainclass=\"\1\"/' $1 | sh
fi
