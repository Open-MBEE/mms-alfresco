package gov.nasa.jpl.view_repo.webscripts.util;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

//import org.json.JSONObject;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.DeclarativeWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

import gov.nasa.jpl.view_repo.connections.ConnectionInterface;
import gov.nasa.jpl.view_repo.connections.JmsConnection;
import gov.nasa.jpl.view_repo.util.Sjm;
import gov.nasa.jpl.view_repo.util.JSONObject;
//import gov.nasa.jpl.view_repo.util.JSONArray;

public class ConnectionPost extends DeclarativeWebScript {
    @Override
    protected Map< String, Object > executeImpl( WebScriptRequest req,
                                                 Status status, Cache cache ) {
        Map<String, Object> model = new HashMap<>();
        JSONObject json = (JSONObject) req.parseContent();


        ConnectionInterface connection = null;

        if (req.getServicePath().endsWith( "jms" )) {
            connection = new JmsConnection();
        }

        JSONObject jsonResponse = new JSONObject();
        if (connection == null) {
            jsonResponse.put( "msg", "connection not found" );
            status.setCode( HttpServletResponse.SC_NOT_FOUND );
        } else {
            connection.ingestJson( json );
            jsonResponse = connection.toJson();
            status.setCode( HttpServletResponse.SC_OK );
        }
        model.put(Sjm.RES, jsonResponse.toString());

        return model;
    }

}
