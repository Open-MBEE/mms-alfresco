<#assign el=args.htmlid?html>

<@markup id="css" >
   <#-- CSS Dependencies -->
   <@link rel="stylesheet" type="text/css" href="${url.context}/res/components/dashlets/docweb.css" group="dashlets"/>
</@>

<@markup id="js">
   <#-- JavaScript Dependencies -->
   <@script type="text/javascript" src="${url.context}/res/components/dashlets/docweb.js" group="dashlets"/>
   <@script type="text/javascript" src="${url.context}/res/modules/simple-dialog.js" group="dashlets"/>
</@>

<@markup id="widgets">
   <#assign id=el?replace("-", "_")>
   <@inlineScript group="dashlets">
      var editDocWebDashletEvent${id} = new YAHOO.util.CustomEvent("onDashletConfigure");
   </@>
   <@createWidgets group="dashlets"/>
   <@inlineScript group="dashlets">
      editDocWebDashletEvent${id}.subscribe(docWeb.onConfigDocWebClick, docWeb, true);
   </@>
</@>

<@markup id="html">
   <@uniqueIdDiv>
   <div class="dashlet docweb <#if isDefault>docweb-mode-default</#if>">
      <div class="title">
         <span id="${args.htmlid}-default-title" class="default-title" target="_blank">${msg("label.header")}</span>
         <span id="${args.htmlid}-notsecure-title" class="notsecure-title" target="_blank">${msg("label.notSecureHeader")}</span>
         <a id="${args.htmlid}-iframe-title" class="iframe-title" target="_blank"></a>
      </div>
      <div class="body scrollablePanel"<#if args.height??> style="height: ${args.height}px;"</#if> id="${args.htmlid}-iframeWrapper">
         <h3 class="configureInstructions default-body">${msg("label.noWebPage")}</h3>
         <h3 class="configureInstructions notsecure-body">${msg("label.notSecurePage")}</h3>
         <iframe id="${args.htmlid}-iframe" class="iframe-body" frameborder="0" scrolling="auto" width="100%" height="100%"></iframe>
         <div class="resize-mask"></div>
      </div>
   </div>
   </@>
</@>
