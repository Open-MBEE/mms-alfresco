package gov.nasa.jpl.view_repo.db;

import com.google.gson.JsonObject;

public class ElasticResult {

    public JsonObject current = null;
    public String elasticId = null;
    public String sysmlid = null;

    @Override
    public String toString() {
        return String.format(
                "sysmlid: %s, elasticId: %s, current: %s",
                sysmlid, elasticId, current);
    }
}
