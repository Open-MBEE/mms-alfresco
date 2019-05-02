<@markup id="css" >
   <#if config.global.header?? && config.global.header.legacyMode && config.global.header.dependencies?? && config.global.header.dependencies.css??>
      <#list config.global.header.dependencies.css as cssFile>
         <@link href="${url.context}/res${cssFile}" group="header"/>
      </#list>
   </#if>
   <style>
       #above-share-header {
           text-align: center;
           color: white;
           margin: auto -10px;
           background-color: red;
       }

       .blink-you {
           animation: blinker 500ms step-start infinite;
       }

       @keyframes blinker {
           50% {
               opacity: 0;
           }
       }
   </style>
</@>

<@markup id="js">
   <#if config.global.header?? && config.global.header.legacyMode && config.global.header.dependencies?? && config.global.header.dependencies.js??>
      <#list config.global.header.dependencies.js as jsFile>
         <@script src="${url.context}/res${jsFile}" group="header"/>
      </#list>
   </#if>
</@>

<@markup id="widgets">
   <@inlineScript group="dashlets">
      <#if page.url.templateArgs.site??>
         Alfresco.constants.DASHLET_RESIZE = ${siteData.userIsSiteManager?string} && YAHOO.env.ua.mobile === null;
      <#else>
         Alfresco.constants.DASHLET_RESIZE = ${((page.url.templateArgs.userid!"-") = (user.name!""))?string} && YAHOO.env.ua.mobile === null;
      </#if>
   </@>
   <@processJsonModel group="share"/>
</@>

<@markup id="html">
   <div id="above-share-header"><span class="blink-you">Please move files to <a href="https://alfresco.jpl.nasa.gov" style="color: white;" target="_blank">OCIO's Alfresco</a></span></div>
   <div id="share-header"></div>
</@>
