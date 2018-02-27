package gov.nasa.jpl.view_repo.webscripts;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import gov.nasa.jpl.view_repo.util.LogUtil;
import gov.nasa.jpl.view_repo.util.Sjm;

/**
 * Utility service for setting log levels of specified classes on the fly
 *
 * @author cinyoung
 */
public class LogLevelPost extends DeclarativeJavaWebScript {
    static Logger logger = Logger.getLogger(LogLevelPost.class);

    @Override protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
        int statusCode = HttpServletResponse.SC_OK;
        Map<String, Object> result = new HashMap<String, Object>();

        StringBuffer msg = new StringBuffer();

        JsonObject response = new JsonObject();

        try {
            JsonParser parser = new JsonParser();
            JsonElement requestJsonElement = parser.parse(req.getContent().getContent());
            JsonArray requestJson = requestJsonElement.getAsJsonArray();

            for (int ii = 0; ii < requestJson.size(); ii++) {
                boolean failed = false;
                JsonObject json = requestJson.get(ii).getAsJsonObject();

                String className = json.get("classname").getAsString();
                String level = json.get("loglevel").getAsString();

                try {
                    Logger classLogger = (Logger) getStaticValue(className, "logger");
                    classLogger.setLevel(Level.toLevel(level));
                } catch (Exception e) {
                    logger.info(String.format("%s", LogUtil.getStackTrace(e)));
                    failed = true;
                }

                if (!failed) {
                    JsonArray logLevels = null;
                    if (!response.has("loglevels")) {
                        logLevels = new JsonArray();
                        response.add("loglevels", logLevels);
                    } else
                        logLevels = response.get("loglevels").getAsJsonArray();
                    try {
                        JsonObject levelObject = new JsonObject();
                        levelObject.addProperty("classname", className);
                        levelObject.addProperty("loglevel", level);
                        logLevels.add(levelObject);
                    } catch (Exception e) {
                        logger.info(String.format("%s", LogUtil.getStackTrace(e)));
                        failed = true;
                    }
                }

                if (failed) {
                    msg.append(String.format("could not update: %s=%s", className, level));
                }
            }
        } catch (IllegalStateException e) {
            statusCode = HttpServletResponse.SC_BAD_REQUEST;
            logger.error(String.format("%s", LogUtil.getStackTrace(e)));
            msg.append("Unable to convert request to JsonArray");
        } catch (JsonParseException e) {
            statusCode = HttpServletResponse.SC_BAD_REQUEST;
            logger.error(String.format("%s", LogUtil.getStackTrace(e)));
            msg.append("Unable to parse request as Json");
        } catch (IOException e) {
            statusCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
            logger.error(String.format("%s", LogUtil.getStackTrace(e)));
            msg.append("Unable to retrieve request content");
        }
        
        if (msg.length() > 0) {
            response.addProperty("msg", msg.toString());
        }
        
        result.put(Sjm.RES, response.toString());
        status.setCode(statusCode);

        return result;
    }

    public static Object getStaticValue(final String className, final String fieldName)
        throws SecurityException, NoSuchFieldException, ClassNotFoundException, IllegalArgumentException,
        IllegalAccessException {
        // Get the private field
        final Field field = Class.forName(className).getDeclaredField(fieldName);
        // Allow modification on the field
        field.setAccessible(true);
        // Return the Obect corresponding to the field
        return field.get(Class.forName(className));
    }
}
