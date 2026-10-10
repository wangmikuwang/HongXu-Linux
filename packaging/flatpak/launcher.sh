#!/bin/sh
# Flatpak launcher: Skia's native library is unpacked into the app's own data folder.
exec /app/jre/bin/java -Dfile.encoding=UTF-8 -Dskiko.data.path="${XDG_DATA_HOME:-$HOME/.local/share}/skiko" -jar /app/lib/hongxu.jar "$@"
