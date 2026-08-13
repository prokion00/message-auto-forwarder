package com.personal.messageforwarder;
import org.json.JSONException;
import org.json.JSONObject;

public final class Rule {
    public String id = String.valueOf(System.currentTimeMillis());
    public String name = "새 규칙", source = "BOTH", destination = "", senders = "";
    public String includeAny = "", includeAll = "", exclude = "";
    public boolean enabled = true;
    JSONObject toJson() throws JSONException {
        JSONObject o=new JSONObject();
        o.put("id",id);o.put("name",name);o.put("source",source);o.put("destination",destination);
        o.put("senders",senders);o.put("includeAny",includeAny);o.put("includeAll",includeAll);o.put("exclude",exclude);o.put("enabled",enabled);return o;
    }
    static Rule fromJson(JSONObject o){Rule r=new Rule();r.id=o.optString("id",r.id);r.name=o.optString("name","규칙");r.source=o.optString("source","BOTH");r.destination=o.optString("destination");r.senders=o.optString("senders");r.includeAny=o.optString("includeAny");r.includeAll=o.optString("includeAll");r.exclude=o.optString("exclude");r.enabled=o.optBoolean("enabled",true);return r;}
}
