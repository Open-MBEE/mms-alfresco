package gov.nasa.jpl.view_repo.db;

import com.google.gson.JsonObject;

public class DocumentResult {

    public JsonObject current = null;
    public String internalId = null;
    public String sysmlid = null;

    @Override
    public String toString() {
        return String.format(
                "sysmlid: %s, internalId: %s, current: %s",
                sysmlid, internalId, current);
    }
}
