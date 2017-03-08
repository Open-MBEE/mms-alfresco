translateOnLoad = false;

jq(document).ready(function() {
    spanclassAM = true;
    mathcolor = "black";
    mathfontsize = "1.2em";
    translate(spanclassAM);

    var script = document.createElement("script");
    script.src = "++resource++fullmarks.mathjax/mathjax/MathJax.js?config=TeX-AMS-MML_HTMLorMML-full"
    // script.src = "http://cdn.mathjax.org/mathjax/latest/MathJax.js?config=TeX-AMS-MML_HTMLorMML-full";
    var config = 'MathJax.Hub.Startup.onload()';
    if (window.opera) {script.innerHTML = config} else {script.text = config}
    document.getElementsByTagName("head")[0].appendChild(script);

});

