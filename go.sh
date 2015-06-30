#!/usr/bin/env sh

# This script is a launcher that compensates for the script code produced by sbt-native-packager, which
# is not ash-compatible (busybox uses ash, not bash).

sed -E 's/app_mainclass=\(\"(.*)\"\)/app_mainclass=\"\1\"/' $1 | sh
