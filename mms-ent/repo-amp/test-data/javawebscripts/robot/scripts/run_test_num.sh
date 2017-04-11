#!/bin/bash

input=$1

numTests=$(echo "$input" | awk -F '[,]' '{print NF-1}')

COUNT=1
catstring="robot"
include='--include'

while [ $COUNT -le $((numTests)) ]; do
        test=$(echo "$input" | cut -f$COUNT -d ,)
        catstring="$catstring $include $test"
        COUNT=$((COUNT+1))
done

catstring="$catstring -L trace -d output regression_test_suite.robot"
$catstring
