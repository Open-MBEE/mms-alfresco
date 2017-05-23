*** Settings ***
Documentation    Testing JMS Operations on Master
Resource        ../resources.robot

*** Test Cases ***
JMS Master Test
    [Tags]    DEBUG
    Log To Console      JMS Master Tests
