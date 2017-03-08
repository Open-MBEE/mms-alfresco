/* Convert input containing ASCIIMath to MathML
*/

tinyMCEPopup.requireLangPack();

translateOnLoad = false;

function displayMathML() {
    if (AMnames.length==0) initSymbols();
    var str = "amath " + document.getElementById("inputText").value;
    var outnode = document.getElementById("outputNode");
    var n = outnode.childNodes.length;
    for (var i=0; i<n; i++)
        outnode.removeChild(outnode.firstChild);
    outnode.appendChild(document.createTextNode(str));
    AMprocessNode(outnode);
}

function insertASCIIMath() {
    val = document.getElementById("inputText").value;
    tinyMCEPopup.restoreSelection();
    // Insert the contents from the input into the document
    tinyMCEPopup.editor.execCommand('mceAsciimath', val);
    tinyMCEPopup.close();
};


function initPopup() {
    var inputText = document.getElementById("inputText");
    inputText.focus();
    inputText.value = tinyMCEPopup.getWindowArg('asciimath');
    displayMathML();
    translate("AM");
}

tinyMCEPopup.onInit.add(initPopup);
