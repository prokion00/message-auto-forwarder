package com.personal.messageforwarder;

import org.json.JSONException;
import org.json.JSONObject;

public final class HistoryItem {
    public String id = "";
    public long createdAt;
    public long updatedAt;
    public String ruleName = "";
    public String source = "";
    public String sender = "";
    public String destination = "";
    public String body = "";
    public String status = "QUEUED";
    public String detail = "";
    public int totalParts = 1;
    public int sentParts = 0;
    public int deliveredParts = 0;

    JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("id", id); o.put("createdAt", createdAt); o.put("updatedAt", updatedAt);
        o.put("ruleName", ruleName); o.put("source", source); o.put("sender", sender);
        o.put("destination", destination); o.put("body", body); o.put("status", status);
        o.put("detail", detail); o.put("totalParts", totalParts);
        o.put("sentParts", sentParts); o.put("deliveredParts", deliveredParts);
        return o;
    }

    static HistoryItem fromJson(JSONObject o) {
        HistoryItem h = new HistoryItem();
        h.id=o.optString("id"); h.createdAt=o.optLong("createdAt"); h.updatedAt=o.optLong("updatedAt");
        h.ruleName=o.optString("ruleName"); h.source=o.optString("source"); h.sender=o.optString("sender");
        h.destination=o.optString("destination"); h.body=o.optString("body"); h.status=o.optString("status","QUEUED");
        h.detail=o.optString("detail"); h.totalParts=o.optInt("totalParts",1);
        h.sentParts=o.optInt("sentParts"); h.deliveredParts=o.optInt("deliveredParts");
        return h;
    }
}
