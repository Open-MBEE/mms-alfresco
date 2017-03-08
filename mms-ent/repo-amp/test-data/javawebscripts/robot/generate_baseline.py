#!/usr/bin/env python

# Libs
import robot_lib
import os

if __name__ == "__main__":

    os.chdir("output/results")
    result_json_files = os.listdir(".")

    for json_file in result_json_files:
        robot_lib.create_baseline_json(json_file)

