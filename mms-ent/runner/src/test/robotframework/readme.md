#How to run Robot Regression Tests

#Requirements

```
pip install robotframework
```

To run the entire suite of tests you can execute the

```
robot -d output suites
```

These encapsulate the robot command
```bash
robot -L trace -d output regression_test_suite.html
```

or you can execute the robot file __be aware there may be an issue with the tabs being interpreted by robot__

```
robot -L trace -d output regression_test_suite.html
```

`-L` specifies the log level

`-d` specifies the output directory __HIGHLY RECOMMEND ALWAYS DOING THIS TO AVOID CLUTTER IN DIRECTORY__

`regression_test_suite.robot` is the robot test suite to execute

you can manually run specific tags by typing

```bash
robot --include ${tagName} -L trace -d output regression_test_suite.robot
```

If you need to generate the original robot file because something happened to

```bash
./generate_robot_tests.py
```
