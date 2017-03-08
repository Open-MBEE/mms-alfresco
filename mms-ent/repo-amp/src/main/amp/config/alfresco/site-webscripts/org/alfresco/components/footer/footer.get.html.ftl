<@markup id="css" >
   <#-- CSS Dependencies -->
   <@link href="${url.context}/res/components/footer/footer.css" group="footer"/>
</@>

<@markup id="js">
   <#-- No JavaScript Dependencies -->
</@>

<@markup id="widgets">
   <@createWidgets/>
</@>

<@markup id="html">
   <@uniqueIdDiv>
      <#assign fc=config.scoped["Edition"]["footer"]>
      <div class="footer ${fc.getChildValue("css-class")!"footer-com"}">
         <span class="copyright">
            <img src="${url.context}/scripts/vieweditor/images/europa-icon.png" width=30 height=26 alt="Europa EMS" title="Europa EMS">
            <span>${msg(fc.getChildValue("label")!"label.copyright")}</span>
         </span>
      </div>
   </@>
</@>
