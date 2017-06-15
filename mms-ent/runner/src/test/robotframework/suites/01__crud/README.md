#CRUD Tests
Directory contains the tests that correspond to CRUD executions

### Tests that are failing because of difference to baseline
1. PostElementsBadOwners
    1. There is no error message in the output
2. GetProject
    1. Returned a 404
3. GetProjects
    1. Returns empty
4. GetElementsRecursively
    1. Returns only 2 elements compared the many that the baseline has
5. GetElementsDepth0
    1. Baseline does not contain Type
    2. Baseline contains _owner, result does not
6. GetElementsDepth1
    1. Result does not return as many elements as the baseline
7. GetElementsDepth2
    1. Result does not return as many elements as the baseline
8. GetElementsDepthAll
    1. Result only contains 1 element while baseline has many
9. GetElementsConnected
    1. Result only contains 2 elements while baseline has many
    2. Result elements contains more information than baseline
10. GetElementsRelationship
    1. Result only contains 1 elements while baseline has many
11. GetViews
    1. Returned a 404 error but still gave the correct JSON response
12. GetSearch
    1. Returns an empty list
13. Delete6666
    1. Baseline seems to be a workspace diff whereas the result seems to be an element get
14. PostDuplicateSysmlNames2
    1. Return a 200 code when it should of been 400
    2. Baseline contains an error message while the result contains an element
15. PostModelForViewDowngrade
    1. Contains an error message and an element while the result only contains an element.
16. PostModelForElementDowngrade
    1. Contains an error message and an element while the result only contains an element.
17. PostNullElements
    1. Returned a 500 when it should of been 200
18. DeleteParents
    1. Returns an empty list
19. TestResurrection2
    1. Missing warning message that baseline contains
20. ParseAndEvaluateTextExressionInFile
    1. Contains commit ID
    2. Returns an empty list
21. CheckIfPostedAspectsInHistory
    1. Returns 404 when it should be 200
22. DeleteElementForAspectHistoryCheck
    1. Baseline workspaces while result contains elements
23. UpdateElementsForAspectHistory
    1. Baseline contains an error message
24. CheckIfAspectUpdatesInHistory
    1. Baseline contains multiple elements while result only contains 1
25. CheckIfAspectDeleteInHistory
    1. Baseline has an error message that the result does not
    2. Result contains an element
26. NA DeleteDeleteAddWsMatrix
    1. Baseline contains workspaces while the result contains elements
27. NA DeleteDeleteUpdateWsMatrix1
    1. Baseline contains workspaces while the result contains elements
28. NA DeleteDeleteDeleteWsMatrix1
    1. Baseline contains workspaces while the result contains elements
29. DeleteUpdateAddMaster
    1. Baseline contains workspaces while the result contains elements
    2.

### Tests that need functionality implementations

1. CreateWorkspaceMatrixTest1
  1. Needs to have the previous time ‘$gv=3’ function to grab info from previous test
2. HierarchyTestPostSite
  1. Is expecting additional arguments that aren’t there
