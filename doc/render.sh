#!/usr/bin/env bash

set -e

# Create output directory if it doesn't exist
mkdir -p renders

# Loop through all .puml files in the current directory
for file in *.puml; do
    # Skip if no .puml files exist
    [ -e "$file" ] || { echo "No .puml files found."; exit 1; }

    echo "Rendering $file..."
    plantuml -teps "$file"

    # Move generated EPS file into the renders folder
    base="${file%.*}"
    mv "${base}.eps" renders/
done

echo "Done! EPS files are in the ./renders directory."
