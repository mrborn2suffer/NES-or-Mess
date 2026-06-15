#!/bin/bash
echo "=== Compiling NES Emulator and ROM Loader ==="
javac *.java

if [ $? -eq 0 ]; then
    echo "=== Running ROM Loader ==="
    java RomLoader
else
    echo "Error: Compilation failed!"
    exit 1
fi
