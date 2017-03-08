var c = sitedata.getComponent(url.templateArgs.componentId);

var uri = String(json.get("url"));
var docwebTitle = String(json.get("docwebTitle"));
c.properties["webviewTitle"] = docwebTitle;
model.docwebTitle = (docwebTitle == "") ? null : docwebTitle;

if (uri !== "")
{
   var re = /^(http|https):\/\//;
   if (!re.test(uri))
   {
      uri = "http://" + uri;
   }
   c.properties["docwebURI"] = uri;
   model.uri = uri;
}

c.save();

