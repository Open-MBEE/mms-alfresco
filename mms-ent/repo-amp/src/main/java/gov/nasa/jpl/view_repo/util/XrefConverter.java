package gov.nasa.jpl.view_repo.util;

import java.util.HashMap;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class XrefConverter {
    private static Map<String, String> OLD_XREF_2_NEW = new HashMap<String, String>() {
        private static final long serialVersionUID = 4377687556311617131L;
        {
            put("name", "name");
            put("documentation", "doc");
            put("value", "val");
        }
    };
    
    public static String convertXref(String html) {
        Document doc = Jsoup.parse( html );
        
        Elements xrefs = doc.select("span.transclusion");
        
        if (xrefs.size() == 0) {
            return html;
        } else {
            for (Element xref: xrefs) {
                String id = xref.attr( "data-mdid" );
                String type = xref.attr( "data-type" );
                String name = xref.attr("data-name");
                
                String newType = OLD_XREF_2_NEW.get( type );
                if (newType != null) {
                    for (Attribute a: xref.attributes()) {
                        xref.removeAttr( a.getKey() );
                    }
    
                    String tagName = String.format("mms-transclude-%s", newType);
                    String innerHtml = String.format("[cf:%s.%s]", name, newType);
                    xref.tagName(tagName);
                    xref.html(innerHtml);
                    xref.attr( "data-mms-eid", id );
                }
            }
                    
            return doc.select("body").html().toString();
        }
    }
    
}
