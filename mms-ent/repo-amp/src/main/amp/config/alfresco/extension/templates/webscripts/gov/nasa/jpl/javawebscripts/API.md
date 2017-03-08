# Retrieve elements

## URLs
* /javawebscripts/elements/{elementid}?recurse={recurse?}
* /javawebscripts/views/{modelid}/elements?recurse={recurse?}

### URL arguments
* elementid = ID for element to retrieve information for
* modelid = ID for view to retrieve element information for
* recurse = [Optional] retrieves elements recursively based on containment if "true"

## Curl Example
Get element with ID 123456 and anything that it contains
* curl -w "%{http_code}" -u cinyoung -k -3 -X GET "https://sheldon/alfresco/service/javawebscripts/elements/123456?recurse=true"

## JSON format
```
      {
              "elements": [
                  {
                      "id": elementId,
                      "type": "Package" | "Property" | "Element" | 
                              "Dependency" | "Generalization" | "DirectedRelationship" | 
                              "Conform" | "Expose" | "Viewpoint",
                      "name": name,
                      "documentation": documentation,
                      "owner": elementId/null,
                      
                      //************** if type is "Property" ******/
                      "propertyType": elementId/null;
                      "isDerived": true|false, 
                      "isSlot": true|false, 
                      "value": [?], (based on valueType)
                      "valueType": "LiteralBoolean" | "LiteralInteger" | "LiteralString" | 
                              "LiteralReal" | "ElementValue" | "Expression",
                      
                      //*************** if type is subtype of DirectedRelationship (Generalization/Expose/Conform/Dependency) ***/
                      "source": elementId,
                      "target": elementId
                      
                      //*************** if type is Comment *******/
                      "body": body
                      "annotatedElements": [elementId, ...]
                  },
                  ...
              ]
          }
```

# Retrieve Views

## URLs
* /javawebscripts/views/{id}?recurse={recurse?}

### URL arguments
* id = ID of view to retrieve information for
* recurse = [Optional] Retrieve any children views if "true"

## Curl Example
Get view info for view with ID 301
* curl -w "%{http_code}" -u cinyoung -k -3 -X GET "https://sheldon/alfresco/service/javawebscripts/views/301"

## JSON Format
```
                {
              "views": 
                  [ {
                      "id": elementId,
                      "displayedElements": [elementId, ...],
                      "allowedElements": [elementId, ..],
                      "childrenViews": [viewId, ..],
                       "contains": [
                           {
                               "type": Paragraph", 
                               "sourceType": "reference"|"text",
                               
                               //*** if sourceType is reference ***/
                               "source": elementId, 
                               "sourceProperty": "documentation"|"name"|"value", 
                               
                               //*** if sourceType is text **/
                               "text": text
                           },
                           {
                               "type": "Table", 
                               "title": title, 
                               "body": [[{
                                   "content": [ //this allows multiple things in a cell
                                       {
                                           "type": "Paragraph"|"List", 
                                           (see above...)
                                       }, ...
                                   ],
                                   //** optional **/
                                   "colspan": colspan, 
                                   "rowspan": rowspan
                               }, ...], ...], 
                               "header": same as body, 
                               //** optional, probably translate to table class if user wants to customize css? **/
                               "style": style
                           },
                           {
                               "type": "List",
                               "list": [
                                   [{ //each list item can have multiple things, Table in a list may not be supported
                                       "type": "Paragraph"/"List"/"Table",
                                       (see above...)
                                   }, ...], ...
                               ],
                               "ordered": true/false
                           }, ...  
                       ],
                  }
              
            ]
          }
```

# Retrieve document list

## URLs
* /ve/documents/{id}

### URL arguments
* id = String of the site name to retrieve all documents for

## Curl Example
Get all documents for europa site (need the format=json request parameters otherwise defaults to HTML)
* curl -w "%{http_code}" -u cinyoung -k -3 -X GET "https://sheldon/alfresco/service/ve/documents/europa?format=json"

## JSON format
```
        {
             "name" : projectname,
             "volumes": {mdid: volumename},
             "volume2volumes": {volumeid: [volumeid, ...], ...},
             "documents": {documentid: documentname, ...},
             "volume2documents": {volumeid: [documentid], ...},
             "projectVolumes": [volumeid,...]
         }
```

# Retrieve Document
Get the entire rendered document view

## URLs
* /ve/products/{id}

### URL arguments
* id = ID of view to retrieve

## Curl Example
Get document view for 123456 (need the format=json request parameters otherwise defaults to HTML)
* curl -w "%{http_code}" -u cinyoung -k -3 -X GET "https://sheldon/alfresco/service/ve/products/123456?format=json"

## JSON format
Combination of the Retrieve Elements and Retrieve Views recursively