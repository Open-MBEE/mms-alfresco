*** Settings ***
Documentation    Testing Workspaces Tags on Master
Resource        ../resources.robot

*** Test Cases ***
Workspaces Tags Master
    [Tags]    DEBUG
    Log To Console      Workspaces Tags Master
