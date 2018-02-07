package gov.nasa.jpl.view_repo.webscripts;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.DeclarativeWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

import gov.nasa.jpl.view_repo.util.LogUtil;
import gov.nasa.jpl.view_repo.util.Sjm;

/**
 * Allows flags to be turned on/off
 */

public abstract class FlagSet extends DeclarativeWebScript {
    static Logger logger = Logger.getLogger(FlagSet.class);

    protected abstract boolean set(boolean val);

    protected abstract boolean get();

    protected abstract boolean get(String flagName);

    protected abstract String flag();

    protected abstract String flagName();

    public abstract String[] getAllFlags();

    protected abstract boolean clear();

    protected WebScriptRequest req = null;

    protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
        FlagSet f = null;
        try {
            f = this.getClass().newInstance();
            f.req = req;
        } catch (InstantiationException | IllegalAccessException e) {
            logger.error(String.format("%s", LogUtil.getStackTrace(e)));
        }
        if (f != null) {
            return f.executeImplImpl(req, status, cache);
        } else {
            return executeImplImpl(req, status, cache);
        }
    }

    protected Map<String, Object> executeImplImpl(WebScriptRequest req, Status status, Cache cache) {
        Map<String, Object> model = new HashMap<>();

        String clear = req.getParameter("clear");
        if (clear != null && !clear.trim().equalsIgnoreCase("false")) {
            boolean didClear = clear();
            if (didClear) {
                model.put(Sjm.RES, "cleared " + flag());
            } else {
                model.put(Sjm.RES, "cannot clear " + flag());
            }
            return model;
        }

        if (flagName().equalsIgnoreCase("all")) {
            // print out all of the flags and their current values
            StringBuffer msg = new StringBuffer();
            msg.append("All flags:\n");
            for (String flag : getAllFlags()) {
                msg.append(flag + " is " + (get(flag) ? "on" : "off") + "\n");
            }
            model.put(Sjm.RES, msg);
            return model;
        }

        String turnOnStr = req.getParameter("on");
        String turnOffStr = req.getParameter("off");

        String isOnStr = null;
        boolean justAsking = false;
        if (turnOnStr == null && turnOffStr == null) {
            isOnStr = req.getParameter("ison");
            if (isOnStr == null) {
                isOnStr = req.getParameter("isOn");
            }
            justAsking = isOnStr != null;
        }

        boolean turnOn =
            !justAsking && !((turnOnStr != null && turnOnStr.trim().equalsIgnoreCase("false")) || (turnOffStr != null
                && !turnOffStr.trim().equalsIgnoreCase("false")));

        String onOrOff = turnOn ? "on" : "off";
        String msg = null;

        if (turnOnStr == null && turnOffStr == null && isOnStr == null) {
            msg = "Parameters are on, off or ison\n" + flagName() + " is " + (get() ? "on" : "off");
        } else if (justAsking) {
            msg = flagName() + " is " + (get() ? "on" : "off");
        } else if (turnOn == get()) {
            msg = flagName() + " is already " + onOrOff;
        } else {
            boolean succ = set(turnOn);
            msg = flagName() + " " + (succ ? "" : "un") + "successfully turned " + onOrOff;
        }
        logger.info(String.format("%s: %s", new Date(), msg));
        model.put(Sjm.RES, msg);

        return model;
    }

}
