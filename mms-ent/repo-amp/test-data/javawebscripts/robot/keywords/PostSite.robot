*** Keywords ***
Baseline PostSite
    [Arguments]                     ${JSON_INPUT}
    Dictionary Should Contain Item  message`/[INFO]: Model folder created.
