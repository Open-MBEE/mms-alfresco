/**
 * ASCIIMath Plugin for TinyMCE editor
 *   based on TinyMCE ASCIIMath plugin written by David Lippman 
 *
 * @author Roché Compaan
 * @copyright Copyright © 2011 Roché Compaan
 *
 */

(function() {
    tinymce.PluginManager.requireLangPack('asciimath');

    tinymce.create('tinymce.plugins.AsciimathPlugin', {
        init : function(ed, url) {
            var t = this;

            ed.addCommand('mceAsciimath', function(val) {

                selected = ed.selection.getNode();
                var AMcontainer = ed.dom.getParent(selected,
                    'span.AMcontainer');
                var spanAM = ed.dom.create('span', {'class' : 'AM'}, val);
                t.ascii2mathml(spanAM);
                var mathml = spanAM.innerHTML;
                mathml = mathml.replace(/>/g,"&gt;");
                mathml = mathml.replace(/</g,"&lt;");

                var cdata = '<![CDATA[' + mathml + ']]>';
                var spanMathML = ed.dom.create(
                    'span', {'class': 'MathML'}, cdata);

                if (AMcontainer) {
                    var tmpAMcontainer = ed.dom.create('span',
                        {'class': 'AMcontainer'});
                    ed.dom.add(tmpAMcontainer, spanAM);
                    ed.dom.add(tmpAMcontainer, spanMathML);
                    AMcontainer.innerHTML = tmpAMcontainer.innerHTML;
                } else {
                    AMcontainer = ed.dom.create('span',
                        {'class': 'AMcontainer'});
                    ed.dom.add(AMcontainer, spanAM);
                    ed.dom.add(AMcontainer, spanMathML);
                    ed.selection.setNode(AMcontainer);
                }
                
            });

            ed.addCommand('mceAsciimathCharmap', function() {
                ed.windowManager.open({
                    file : url + '/amcharmap',
                    width : 630 + parseInt(
                        ed.getLang('asciimathdlg.delta_width', 0)),
                    height : 390 + parseInt(
                        ed.getLang('asciimathdlg.delta_height', 0)),
                    inline : 1
                }, {
                    plugin_url : url, // Plugin absolute URL
                });
                
            });

            ed.addCommand('mceAsciimathPopup', function() {
                var el = ed.selection.getNode();
                var spanAM = ed.dom.getParent(el, 'span.AM');
                var asciimath = "";

                if (spanAM) {
                    mathml = spanAM.cloneNode(true);
                    t.math2ascii(mathml); 
                    asciimath = mathml.innerHTML.slice(1,-1);
                }

                ed.windowManager.open({
                    file : url + '/ampopup.htm',
                    width : 630 + parseInt(
                        ed.getLang('asciimathdlg.delta_width', 0)),
                    height : 390 + parseInt(
                        ed.getLang('asciimathdlg.delta_height', 0)),
                    inline : 1
                }, {
                    plugin_url : url, // Plugin absolute URL
                    asciimath: asciimath,
                });
                
            });

            // Add a node change handler, selects the button in the UI
            // when mathml is selected
            ed.onNodeChange.add(function(ed, cm, n) {
                selected = ed.dom.select('math.mceItemVisualAid');
                for (var i=0; i < selected.length; i++) {
                    math = selected[i];
                    math.removeAttribute('class');
                };
                var AMspan = ed.dom.getParent(n, 'span.AM');
                cm.setActive('asciimath', AMspan != null);
                if (AMspan) {
                    math = AMspan.getElementsByTagName('math')[0];
                    // force selection of the math element since
                    // selection of child elements causes an exception when
                    // TinyMCE tries access the style attribute on those
                    // MathML elements
                    ed.selection.select(math);
                    ed.selection.collapse(true);
                    // highlight the math element
                    if (AMspan.getElementsByClassName('mceItemVisualAid')) {
                        if (math != null) {
                            // not sure why ed.dom.addClass does not work
                            // ed.dom.addClass(math, 'mceItemVisualAid');
                            math.setAttribute('class', 'mceItemVisualAid');
                        }
                    };

                }
            });

            ed.onKeyPress.add(function(ed, e) {
                // delete MathML when delete or backspace key is pressed
                if (e.keyCode == 46 || e.keyCode == 8) {
                    node = ed.selection.getNode();
                    var AMcontainer = ed.dom.getParent(node, 'span.AMcontainer');
                    if (AMcontainer) {
                        AMcontainer.parentNode.removeChild(AMcontainer);
                    }
                }

                // place the caret after the MathML node when pressing
                // enter, spacebar, down or right arrow
                if (e.keyCode == 13 || e.keyCode == 0 ||
                    e.keyCode == 37 || e.keyCode == 38 ||
                    e.keyCode == 39 || e.keyCode == 40) {
                    var rng, AMcontainer, dom = ed.dom;

                    rng = ed.selection.getRng();
                    AMcontainer = dom.getParent(rng.startContainer, 'span.AMcontainer');

                    if (AMcontainer) {
                        rng = dom.createRng();

                        if (e.keyCode == 37 || e.keyCode == 38) {
                            rng.setStartBefore(AMcontainer);
                            rng.setEndBefore(AMcontainer);
                        } else {
                            rng.setStartAfter(AMcontainer);
                            rng.setEndAfter(AMcontainer);
                        }
                        ed.selection.setRng(rng);
                    }
                }
            });


            // Fix caret position
            ed.onInit.add(function(ed) {
                if (!tinymce.isIE) {
                    function fixCaretPos() {
                        var last = ed.getBody().lastChild;
                        if (last && last.nodeName == 'SPAN' && last.className =='AMcontainer') {
                            br = ed.dom.create('br', {'mce_bogus' : '1'});
                            ed.getBody().appendChild(br);
                        }
                    };
                    fixCaretPos();
                };
                ed.onKeyUp.add(fixCaretPos);
                ed.onSetContent.add(fixCaretPos);
                ed.onVisualAid.add(fixCaretPos);
            });


            // Register asciimath button
            ed.addButton('asciimath', {
                title : 'asciimath.desc',
                cmd : 'mceAsciimathPopup',
                image : url + '/img/ed_mathformula2.gif'
            });


            ed.addButton('asciimathcharmap', {
                title : 'asciimathcharmap.desc',
                cmd : 'mceAsciimathCharmap',
                image : url + '/img/ed_mathformula.gif'
            });

            ed.onPreInit.add(function(ed) {
                if (tinymce.isIE) {
                    addhtml = "<object id=\"mathplayer\" classid=\"clsid:32F66A20-7614-11D4-BD11-00104BD3F987\"></object>";
                    addhtml +="<?import namespace=\"m\" implementation=\"#mathplayer\"?>";
            
                    ed.dom.doc.getElementsByTagName("head")[0].insertAdjacentHTML("beforeEnd",addhtml);
                }
                
            });

            ed.onPreProcess.add(function(ed,o) {
                if (o.get) {
                    AMtags = ed.dom.select('span.AM', o.node);
                    for (var i=0; i<AMtags.length; i++) {
                        t.math2ascii(AMtags[i]); 
                    }
                    AMtags = ed.dom.select('span.AMedit', o.node);
                    for (var i=0; i<AMtags.length; i++) {
                        var myAM = AMtags[i].innerHTML;
                        myAM = "`"+myAM.replace(/\`/g,"")+"`";
                        AMtags[i].innerHTML = myAM;
                        AMtags[i].className = "AM";
                    }
                } 
            });

            ed.onBeforeGetContent.add(function(ed,cmd) {
                AMtags = ed.dom.select('span.AM');
                for (var i=0; i<AMtags.length; i++) {
                    t.math2ascii(AMtags[i]);
                    AMtags[i].className = "AMedit";
                }
            });

            ed.onGetContent.add(function(ed,cmd) {
                AMtags = ed.dom.select('span.AMedit');
                for (var i=0; i<AMtags.length; i++) {
                    t.ascii2mathml(AMtags[i]);
                    AMtags[i].className = "AM";
                }
            });

            ed.onExecCommand.add(function(ed,cmd) {
                if (cmd == 'mceRepaint') {
                    AMtags = ed.dom.select('span.AM');
                    for (var i=0; i<AMtags.length; i++) {
                        t.ascii2mathml(AMtags[i]);
                    }
                }
            });

        },

        getInfo : function() {
            return {
                longname : 'Asciimath plugin',
                author : 'Roché Compaan',
                authorurl : 'http://github.com/rochecompaan',
                infourl : '',
                version : "1.0"
            };
        },

        math2ascii : function(el) {
            // ASCIIMath is stored in title attribute
            var myAM = el.innerHTML;
            if (myAM.indexOf("`") == -1) {
                myAM = myAM.replace(/.+(alt|title)=\"(.*?)\".+/g,"$2");
                myAM = myAM.replace(/.+(alt|title)=\'(.*?)\'.+/g,"$2");
                myAM = myAM.replace(/.+(alt|title)=([^>]*?)\s.*>.*/g,"$2");
                myAM = myAM.replace(/.+(alt|title)=(.*?)>.*/g,"$2");
                //myAM = myAM.replace(/&gt;/g,">");
                //myAM = myAM.replace(/&lt;/g,"<");
                myAM = myAM.replace(/>/g,"&gt;");
                myAM = myAM.replace(/</g,"&lt;");
                myAM = "`"+myAM.replace(/\`/g,"")+"`";
                el.innerHTML = myAM;
            }
        },

        ascii2mathml : function(outnode) {
            
            if (tinymce.isIE) {
                  var str = outnode.innerHTML.replace(/\`/g,"");
                  str.replace(/\"/,"&quot;");
                  var newAM = document.createElement("span");
                  newAM.appendChild(AMparseMath(str));
                  outnode.innerHTML = newAM.innerHTML;    
              } else {
                  // doesn't work on IE, probably because this script is
                  // in the parent
                  // windows, and the node is in the iframe.  Should it
                  // work in Moz?

                 // next 2 lines needed to make caret
                 var myAM = "`"+outnode.innerHTML.replace(/\`/g,"")+"`"; 

                 // move between `` on Firefox insert math
                 outnode.innerHTML = myAM;     
                 AMprocessNode(outnode);
              }
            
        }, 

        testAMclass : function(el) {
            if ((el.className == 'AM') || (el.className == 'AMedit')) {
                return true;
            } else {
                return false;
            }
        }
    });

    // Register plugin
    tinymce.PluginManager.add('asciimath', tinymce.plugins.AsciimathPlugin);
})();
