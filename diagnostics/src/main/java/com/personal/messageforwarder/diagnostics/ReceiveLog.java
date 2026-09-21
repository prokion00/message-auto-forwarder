package com.personal.messageforwarder.diagnostics;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;

final class ReceiveLog {
    static SharedPreferences prefs(Context c) { return c.getSharedPreferences("receive", Context.MODE_PRIVATE); }
    static synchronized void add(Context c, String id, String source, String sender, String body, String note) {
        addWithAddress(c, id, source, sender, body, "", note);
    }
    static synchronized void addWithAddress(Context c, String id, String source, String sender, String body, String address, String note) {
        record(c,id,source,sender,body,address,note,0,false);
    }
    static synchronized void record(Context c,String id,String source,String sender,String body,String address,String note,long eventAt,boolean uncertain) {
        try {
            SharedPreferences p = prefs(c);
            JSONArray old = new JSONArray(p.getString("events", "[]"));
            JSONObject item = new JSONObject().put("id", id).put("source", source)
                .put("sender", sender == null ? "" : sender).put("body", body == null ? "" : body)
                .put("address", address).put("note", note).put("time", System.currentTimeMillis()).put("eventAt",eventAt).put("uncertain",uncertain);
            JSONArray next = new JSONArray();
            for (int i = 0; i < old.length(); i++) {
                JSONObject v = old.getJSONObject(i);
                if (id.equals(v.optString("id")) && item.optString("body").equals(v.optString("body"))
                    && item.optString("sender").equals(v.optString("sender")) && address.equals(v.optString("address")) && note.equals(v.optString("note"))) return;
            }
            next.put(item);
            for (int i = 0; i < old.length() && next.length() < 50; i++) {
                JSONObject v = old.getJSONObject(i);
                if (!id.equals(v.optString("id"))) next.put(v);
            }
            p.edit().putString("events", next.toString()).apply();
            AutoForwarder.submit(c,item);
        } catch (org.json.JSONException ignored) { }
    }
    static String display(Context c) {
        StringBuilder out = new StringBuilder();
        try {
            JSONArray rows = new JSONArray(prefs(c).getString("events", "[]"));
            for (int i = 0; i < rows.length(); i++) {
                JSONObject v = rows.getJSONObject(i);

                out.append("\n[").append(v.optString("source")).append("] ")
                    .append(android.text.format.DateFormat.format("MM-dd HH:mm:ss", v.optLong("time")))
                    .append("\n발신인: ").append(v.optString("sender", "확인 불가"))
                    .append(v.optString("address").isEmpty() ? "" : "\n발신번호: " + v.optString("address")).append("\n본문: ").append(v.optString("body"))
                    .append("\n").append(v.optString("note")).append("\n").append(Rules.preview(c, v)).append("\n────────────\n");
            }
        } catch (org.json.JSONException ignored) { }
        return out.length() == 0 ? "아직 감지된 메시지가 없습니다." : out.toString();
    }
}



