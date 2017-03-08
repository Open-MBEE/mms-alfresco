function main()
{
   var height = (args.editorHeight != null) ? args.editorHeight : 250,
         width = (args.editorWidth != null) ? args.editorWidth : 538,
         locale = locale.substring(0, 2);

   var createComment = {
      id: "CreateComment",
      name : "Alfresco.CreateComment",
      options : {
         siteId : (page.url.templateArgs.site != null) ? page.url.templateArgs.site : "",
         containerId : template.properties.container != null ? template.properties.container : "documentLibrary",
         height : height,
         width : width,
         editorConfig : {
            height: height,
            width: width,
            inline_styles: false,
            convert_fonts_to_spans: false,
            theme: "advanced",
            plugins: "asciimath,table,visualchars,emotions,advhr,print,directionality,fullscreen,insertdatetime",
            theme_advanced_buttons1: "bold,italic,underline,strikethrough,|,justifyleft,justifycenter,justifyright,justifyfull,|,formatselect,fontselect,fontsizeselect,forecolor,backcolor",
            theme_advanced_buttons2: "bullist,numlist,|,outdent,indent,blockquote,|,undo,redo,|,link,unlink,anchor,alfresco-imagelibrary,alfresco-linklibrary,image,cleanup,help,code,removeformat,|,insertdate,inserttime",
            theme_advanced_buttons3: "tablecontrols,|,hr,removeformat,visualaid,|,sub,sup,|,charmap,asciimath,emotions,advhr,|,print,|,ltr,rtl,|,fullscreen",
            theme_advanced_toolbar_location: "top",
            theme_advanced_toolbar_align: "left",
            theme_advanced_statusbar_location: "bottom",
            theme_advanced_path : false,
            theme_advanced_resizing: true,
            language: locale
         }
      }
   };
   model.widgets = [createComment];
   odel.widgets.push();
}

main();

