function main()
{
   var uri = args.docwebURI,
      docwebTitle = '',
      isDefault = false;

   var connector = remote.connect("alfresco");
   var result = connector.get("/javawebscripts/hostname");
   
   var data = result.response + ' ';
   if(data == null){
       throw new Error('Unable to retrieve host information.');
       return;
   }

   var json = jsonUtils.toObject(data);
   if(json == null){
       throw new Error('Unable to parse host information JSON.');
       return;
   }
   
   var hostname = json.alfresco.host;
   if(hostname.toLowerCase()=='localhost') hostname += ':' + json.alfresco.port;
   hostname = json.alfresco.protocol + '://' + hostname;
   
   var siteName = page.url.templateArgs.site;
   if (siteName)
   {
       uri = hostname + '/alfresco/mmsapp/mms.html#/workspaces/master/sites/' + siteName;
       docwebTitle = siteName + ' Docweb';
   }
   else{
       uri = hostname + '/alfresco/mmsapp/mms.html#/workspaces/master';
       docwebTitle = 'Docweb Portal';
   }
   
   var height = args.height;
   if (!height)
   {
      height = "";
   }

   var re = /^(http|https):\/\//;

   if (!isDefault && !re.test(uri))
   {
      uri = "http://" + uri;
   }

   model.docwebTitle = docwebTitle;
   model.uri = uri;
   model.height = height;
   model.isDefault = isDefault;

   /*var userIsSiteManager = true;
   if (page.url.templateArgs.site)
   {
      // We are in the context of a site, so call the repository to see if the user is site manager or not
      userIsSiteManager = false;
      var json = remote.call("/api/sites/" + page.url.templateArgs.site + "/memberships/" + encodeURIComponent(user.name));

      if (json.status == 200)
      {
         var obj = eval('(' + json + ')');
         if (obj)
         {
            userIsSiteManager = (obj.role == "SiteManager");
         }
      }
   }
   model.userIsSiteManager = userIsSiteManager;*/
   
   // Widget instantiation metadata...
   var docWeb = {
      id : "DocWeb", 
      name : "Alfresco.dashlet.DocWeb",
      assignTo : "docWeb",
      options : {
         componentId : instance.object.id,
         docwebURI : model.uri,
         docwebTitle : model.docwebTitle,
         docwebHeight : model.height,
         isDefault : model.isDefault
      }
   };

   var dashletResizer = {
      id : "DashletResizer", 
      name : "Alfresco.widget.DashletResizer",
      initArgs : ["\"" + args.htmlid + "\"", "\"" + instance.object.id + "\""],
      useMessages: false
   };

   var actions = [];
   if (model.userIsSiteManager)
   {
      actions.push(
      {
         cssClass: "edit",
         eventOnClick: {
            _alfValue : "editDocWebDashletEvent" + args.htmlid.replace(/-/g, "_"),
            _alfType: "REFERENCE"
         }, 
         tooltip: msg.get("dashlet.edit.tooltip")
      });
   }
   actions.push({
      cssClass: "help",
      bubbleOnClick:
      {
         message: msg.get("dashlet.help")
      },
      tooltip: msg.get("dashlet.help.tooltip")
   });
   
   var dashletTitleBarActions = {
      id : "DashletTitleBarActions", 
      name : "Alfresco.widget.DashletTitleBarActions",
      useMessages : false,
      options : {
         actions: actions
      }
   };
   model.widgets = [docWeb, dashletResizer, dashletTitleBarActions];
}

main();
