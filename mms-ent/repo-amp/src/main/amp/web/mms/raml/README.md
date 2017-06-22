A combination of RAML and JSON Schema are used to specify the API while
SOAPUI with the RAML plugin is used for testing and mocking of the service
APIs.

File Structure
--------------
* api.raml - the raml file to add/edit endpoints
* *.json-schema - the JSON schema files for the resrouce types
* index.html - along with the fonts/scripts/styles/directories so everything can be viewed interactively in the browser i.e {some server}/alfresco/mms/raml/index.html
* local.html - used for interactive browsing locally of api.raml

RAML Specification
------------------

* [RAML Specification](http://raml.org/spec.html)
* [RAML Projects](http://raml.org/projects.html)
  * [API Console](https://github.com/mulesoft/api-console)
    * Javadoc like tool for creating HTML documentation for RAML API
    * Also, provided in HTML directory, use local.html and paste in RAML file, you'll need
      to remove the schema references to just '|'
    * Currently deployed to [Crushinator RAML](https://128.149.16.152:8443/alfresco/scripts/raml/index.html)
  * [API Designer](https://github.com/mulesoft/api-designer)
    * This is a browser (default port 3000) based editor that checks RAML syntax.  This tool is only needed to set up the project locally.
    * Web-based design tool for RAML that includes API Console as a widget
    * **WARNING:** do not enable the Mocking Service as it publishes information to the cloud

JSON Schema
-----------

* [JSON Schema Specification](http://json-schema.org/documentation.html)
* [JSON Schema Examples](http://json-schema.org/examples.html)
