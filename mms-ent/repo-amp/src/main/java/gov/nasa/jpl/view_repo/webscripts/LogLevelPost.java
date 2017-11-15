package gov.nasa.jpl.view_repo.webscripts;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import gov.nasa.jpl.view_repo.util.SerialJSONArray;
import org.json.JSONException;
import gov.nasa.jpl.view_repo.util.SerialJSONObject;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

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
        Map<String, Object> result = new HashMap<>();

        StringBuffer msg = new StringBuffer();

        SerialJSONObject response = new SerialJSONObject();

        SerialJSONArray requestJson;
        try {
            requestJson = new SerialJSONArray(req.parseContent());
        } catch (JSONException e) {
            status.setCode(HttpServletResponse.SC_BAD_REQUEST);
            response.put("msg", "JSON malformed");
            result.put(Sjm.RES, response);
            return result;
        }

        for (int ii = 0; ii < requestJson.length(); ii++) {
            boolean failed = false;
            SerialJSONObject json = requestJson.getJSONObject(ii);

            String className = json.getString("classname");
            String level = json.getString("loglevel");

            try {
                Logger classLogger = (Logger) getStaticValue(className, "logger");
                classLogger.setLevel(Level.toLevel(level));
            } catch (Exception e) {
                logger.info(String.format("%s", LogUtil.getStackTrace(e)));
                failed = true;
            }

            if (!failed) {
                if (!response.has("loglevels")) {
                    response.put("loglevels", new SerialJSONArray());
                }
                try {
                    SerialJSONObject levelObject = new SerialJSONObject();
                    levelObject.put("classname", className);
                    levelObject.put("loglevel", level);
                    response.getJSONArray("loglevels").put(levelObject);
                } catch (Exception e) {
                    logger.info(String.format("%s", LogUtil.getStackTrace(e)));
                    failed = true;
                }
            }

            if (failed) {
                msg.append(String.format("could not update: %s=%s", className, level));
            }
        }

        if (msg.length() > 0) {
            response.put("msg", msg.toString());
        }
        result.put(Sjm.RES, response.toString(4));
        status.setCode(HttpServletResponse.SC_OK);

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
