<@markup id="css" >
<#-- CSS Dependencies -->
<@link href="${url.context}/res/components/guest/login.css" group="login"/>
</@>

<@markup id="js">
<#-- JavaScript Dependencies -->
<@script src="${url.context}/res/components/guest/login.js" group="login"/>
<@inlineScript>
window.onload=function() {
var style = "background-color:#000000;";
document.body.setAttribute("style", style);
var elems = document.getElementsByClassName('sticky-wrapper');
for (var ii in elems) {
elems[ii].setAttribute("style", style);
}
};
</@>
</@>

<@markup id="widgets">
<@createWidgets group="login"/>
</@>

<@markup id="html">
<@uniqueIdDiv>
<#assign el=args.htmlid?html>
<div id="${el}-body" class="theme-overlay login hidden">
    
    <table>
        <tbody>
            <tr>
                <td>
                </td>
                <td>
                    <span style="font-size:30px;"><b>OpenCAE</b></span>
                </td>
            </tr>
        </tbody>
    </table>
    
    <#if errorDisplay == "container">
    <@markup id="error">
    <#if error>
    <div class="error">${msg("message.loginautherror")}</div>
    <#else>
    <script type="text/javascript">//<![CDATA[
            document.cookie = "_alfTest=_alfTest";
            var cookieEnabled = (document.cookie.indexOf("_alfTest") !== -1);
            if (!cookieEnabled)
            {
               document.write('<div class="error">${msg("message.cookieserror")}</div>');
            }
         //]]></script>
    </#if>
    </@markup>
    </#if>
    
    <@markup id="form">
    <form id="${el}-form" accept-charset="UTF-8" method="post" action="${loginUrl}" class="form-fields">
        <@markup id="fields">
        <input type="hidden" id="${el}-success" name="success" value="${successUrl?html}"/>
        <input type="hidden" name="failure" value="${failureUrl?html}"/>
        <div class="form-field">
            <label for="${el}-username">${msg("label.username")}</label><br/>
            <input type="text" id="${el}-username" name="username" maxlength="255" value="<#if lastUsername??>${lastUsername?html}</#if>" />
        </div>
        <div class="form-field">
            <label for="${el}-password">${msg("label.password")}</label><br/>
            <input type="password" id="${el}-password" name="password" maxlength="255" />
        </div>
        </@markup>
        <@markup id="buttons">
        <div class="form-field">
            <input type="submit" id="${el}-submit" class="login-button" value="${msg("button.login")}"/>
        </div>
        </@markup>
    </form>
    </@markup>
    
    <!--      
      <@markup id="footer">
         <span class="faded tiny">JPL/Caltech BUSINESS DISCREET. Caltech Record. Not for Public Distribution.</span>
      </@markup>
-->
    
</div>
</@>
</@>
