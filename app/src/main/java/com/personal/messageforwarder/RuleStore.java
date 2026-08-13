package com.personal.messageforwarder;
import android.content.Context;
import org.json.JSONArray;
import java.util.ArrayList;
import java.util.List;

public final class RuleStore {
    private static final String PREFS="forwarder_rules", KEY="rules";
    public static synchronized List<Rule> load(Context c){
        List<Rule> out=new ArrayList<>();String raw=c.getSharedPreferences(PREFS,0).getString(KEY,"[]");
        try{JSONArray a=new JSONArray(raw);for(int i=0;i<a.length();i++)out.add(Rule.fromJson(a.getJSONObject(i)));}catch(Exception ignored){}return out;
    }
    public static synchronized void save(Context c,List<Rule> rules){JSONArray a=new JSONArray();try{for(Rule r:rules)a.put(r.toJson());}catch(Exception ignored){}c.getSharedPreferences(PREFS,0).edit().putString(KEY,a.toString()).apply();}
}
