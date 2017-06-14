#!/usr/bin/env bash

psql -t -U mmsuser mms -c "SELECT projectid FROM projects" | awk '{print $1}' > projectids.txt

while IFS= read -r line; do

    if [[ "${line// }" ]]; then
        dropdb -U mmsuser "_$line";
    fi

done < projectids.txt

rm projectids.txt
