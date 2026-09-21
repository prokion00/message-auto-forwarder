package com.personal.messageforwarder.diagnostics;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;

final class Rules {
    static JSONArray load(Context c) {
        try { return new JSONArray(c.getSharedPreferences("rules",0).getString("items","[]")); }
        catch(org.json.JSONException e){return new JSONArray();}
    }
    static boolean save(Context c,JSONArray items){return c.getSharedPreferences("rules",0).edit().putString("items",items.toString()).commit();}
    static String reason(JSONObject r,JSONObject event){
        if(!r.optBoolean("enabled",true))return "규칙 꺼짐";
        String source=event.optString("source");
        if(source.equals("카카오톡 알림"))source="카카오톡";
        if(source.startsWith("메시지 앱 알림"))source="메시지 알림";
        String sender=event.optString("sender"),note=event.optString("note");
        boolean uncertain=event.optBoolean("uncertain",false)||sender.startsWith("발신인 후보:")||sender.startsWith("발신인 미확정")
            ||sender.equals("확인 불가")||note.contains("일부 텍스트를 읽을 수 없음");
        String result=RuleMatcher.evaluate(r.optString("channel"),r.optString("senders"),r.optString("all"),r.optString("any"),r.optString("exclude"),source,sender,event.optString("body"),uncertain);
        if(result.equals("발신인 불일치")&&source.equals("메시지 알림")&&!event.optString("address").isEmpty())
            result=RuleMatcher.evaluate(r.optString("channel"),r.optString("senders"),r.optString("all"),r.optString("any"),r.optString("exclude"),source,event.optString("address"),event.optString("body"),uncertain);
        return result;
    }
    static String preview(Context c,JSONObject event){
        JSONArray rules=load(c);StringBuilder out=new StringBuilder("현재 규칙 판별 (발송 결과는 자동발송 내역에서 확인)");
        if(rules.length()==0)return out+"\n등록된 규칙 없음";
        for(int i=0;i<rules.length();i++){
            JSONObject r=rules.optJSONObject(i);if(r==null)continue;
            String result=reason(r,event);
            out.append("\n").append(r.optString("name")).append(": ").append(result);
            if(result.equals("일치"))out.append(" → 대상 ").append(RuleMatcher.terms(r.optString("destinations")).size()).append("개\n전송 본문:\n").append(OutgoingMessage.format(event.optString("body")));
        }
        return out.toString();
    }
}
