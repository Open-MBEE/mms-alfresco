A combination of RAML and JSON Schema are used to specify the API while
SOAPUI with the RAML plugin is used for testing and mocking of the service
APIs.

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
    * Web-based design tool for RAML that includes API Console as a widget
    * **WARNING:** do not enable the Mocking Service as it publishes information to the cloud
  
JSON Schema
-----------

* [JSON Schema Specification](http://json-schema.org/documentation.html)
* [JSON Schema Examples](http://json-schema.org/examples.html)

SoapUI
------

* [SoapUI](http://www.soapui.org)
  * [Download SoapUI](http://sourceforge.nt/projects/soapui/files)
* [RAML Plugin](http://olensmar.blogspot.se/2013/12/a-raml-apihub-plugin-for-soapui.html)
  * [Download RAML Plugin](http://sourceforge.net/projects/soapui-plugins/files/soapui-raml-plugin/)