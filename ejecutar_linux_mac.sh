#!/usr/bin/env bash
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
cd "$DIR"
if [ -f "$DIR/AlmacenApp.jar" ]; then
    java -jar "$DIR/AlmacenApp.jar" "$@"
else
    java -cp "$DIR/bin:$DIR/lib/hsqldb.jar" almacen.Principal "$@"
fi
