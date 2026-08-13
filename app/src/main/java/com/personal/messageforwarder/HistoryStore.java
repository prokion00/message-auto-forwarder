package com.personal.messageforwarder;

import android.content.Context;
import org.json.JSONArray;
import java.util.ArrayList;
import java.util.List;

public final class HistoryStore {
    private static final String PREFS="forwarder_history", KEY="items";
    private static final int MAX_ITEMS=200;

    public static synchronized List<HistoryItem> load(Context c) {
        List<HistoryItem> out=new ArrayList<>();
        String raw=c.getSharedPreferences(PREFS,0).getString(KEY,"[]");
        try { JSONArray a=new JSONArray(raw); for(int i=0;i<a.length();i++) out.add(HistoryItem.fromJson(a.getJSONObject(i))); }
        catch(Exception ignored) { }
        return out;
    }

    public static synchronized void add(Context c, HistoryItem item) {
        List<HistoryItem> items=load(c); items.add(0,item);
        while(items.size()>MAX_ITEMS) items.remove(items.size()-1);
        save(c,items);
    }

    public static synchronized void updateResult(Context c,String id,String type,boolean success,String detail) {
        List<HistoryItem> items=load(c);
        for(HistoryItem h:items) if(h.id.equals(id)) {
            h.updatedAt=System.currentTimeMillis();
            if(!success) { h.status=type.equals("SENT")?"SEND_FAILED":"DELIVERY_FAILED"; h.detail=detail; }
            else if(type.equals("SENT")) { h.sentParts++; if(h.sentParts>=h.totalParts) { h.status="SENT"; h.detail="통신사 발송 요청 접수 완료"; } }
            else { h.deliveredParts++; if(h.deliveredParts>=h.totalParts) { h.status="DELIVERED"; h.detail="수신 단말 전달 완료"; } }
            save(c,items); return;
        }
    }

    public static synchronized void clear(Context c) { c.getSharedPreferences(PREFS,0).edit().remove(KEY).apply(); }

    private static void save(Context c,List<HistoryItem> items) {
        JSONArray a=new JSONArray(); try { for(HistoryItem h:items)a.put(h.toJson()); } catch(Exception ignored) { }
        c.getSharedPreferences(PREFS,0).edit().putString(KEY,a.toString()).apply();
    }
}
