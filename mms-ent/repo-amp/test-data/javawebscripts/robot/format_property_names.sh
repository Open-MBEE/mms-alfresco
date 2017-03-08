#!/bin/bash

find . -type f \( -iname \*.json \) -exec sed -i '' 's/\"owner\"/\"_owner\"/g' {} \;
find . -type f \( -iname \*.json \) -exec sed -i '' 's/\"modified\"/\"_modified\"/g' {} \;
find . -type f \( -iname \*.json \) -exec sed -i '' 's/\"modifier\"/\"_modifier\"/g' {} \;
find . -type f \( -iname \*.json \) -exec sed -i '' 's/\"editable\"/\"_editable\"/g' {} \;
find . -type f \( -iname \*.json \) -exec sed -i '' 's/\"created\"/\"_created\"/g' {} \;
find . -type f \( -iname \*.json \) -exec sed -i '' 's/\"creator\"/\"_creator\"/g' {} \;
find . -type f \( -iname \*.json \) -exec sed -i '' 's/\"sysmlid\"/\"sysmlId\"/g' {} \;

# Remove qualifiedName
find . -type f \( -iname \*.json \) -exec sed -i '' '/\"qualifiedName\"/d' {} \;
find . -type f \( -iname \*.json \) -exec sed -i '' '/\"qualifiedId\"/d' {} \;