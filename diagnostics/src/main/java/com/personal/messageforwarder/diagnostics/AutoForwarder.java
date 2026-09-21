package com.personal.messageforwarder.diagnostics;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.telephony.SmsManager;
import android.telephony.SubscriptionManager;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.*;
import java.util.concurrent.Executors;

final class AutoForwarder {
    private static final java.util.concurrent.ExecutorService worker=Executors.newSingleThreadExecutor();
    static SharedPreferences prefs(Context c){ return c.getSharedPreferences("automatic",0); }
    static boolean enabled(Context c){ return prefs(c).getBoolean("enabled",false); }
    static void update(Context c,String text){ prefs(c).edit().putString("last",text).putLong("changed",System.currentTimeMillis()).apply(); }
    static void submit(Context c,JSONObject event){
        Context app=c.getApplicationContext();
        worker.execute(() -> {
            try { process(app,event); } catch (RuntimeException e) { update(app,"처리 오류: " + e.getClass().getSimpleName() + " · 자동 재시도 없음"); }
        });
    }
    static String hash(String value){
        try { byte[] bytes=java.security.MessageDigest.getInstance("SHA-256").digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder out=new StringBuilder(); for(byte b:bytes)out.append(String.format(java.util.Locale.ROOT,"%02x",b));return out.toString();
        } catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}
    }
    static LinkedHashMap<String,String> targets(JSONArray rules, JSONObject event) {
        LinkedHashMap<String,String> out=new LinkedHashMap<>();
        for(int i=0;i<rules.length();i++) {
            JSONObject r=rules.optJSONObject(i); if(r==null || !Rules.reason(r,event).equals("일치"))continue;
            for(String raw:RuleMatcher.terms(r.optString("destinations"))) {
                String n=RuleMatcher.number(raw);
                if(!n.matches("\\+?[0-9]{8,15}")) continue;
                out.merge(n,r.optString("name"),(a,b)->a+", "+b);
            }
        }
        return out;
    }
    private static void process(Context c,JSONObject event) {
        if(!enabled(c)||!AutoService.running)return;
        String body=event.optString("body");
        String blocked=AutoPolicy.blocked(body,event.optLong("eventAt"),prefs(c).getLong("enabledAt",Long.MAX_VALUE));
        if(!blocked.isEmpty()) { update(c,blocked); return; }
        LinkedHashMap<String,String> targets=targets(Rules.load(c),event);
        if(targets.isEmpty()){update(c,"조건에 일치하는 전송 대상 없음");return;}
        int sub=prefs(c).getInt("subscription",SubscriptionManager.INVALID_SUBSCRIPTION_ID);
        String eventKey=hash(event.optString("id")+"\n"+body);
        try(AutoStore store=new AutoStore(c)) {
            for(Map.Entry<String,String> target:targets.entrySet()) {
                if(!enabled(c)||!AutoService.running)return;
                String id=UUID.randomUUID().toString();
                // Same body to the same recipient within 2 minutes is suppressed across sources.
                if(!store.reserve(id,eventKey,target.getKey(),hash(body+"\n"+target.getKey()),System.currentTimeMillis(),event.optString("source"),target.getValue())) {
                    update(c,"중복 발송 차단 (같은 대상·본문 또는 이미 처리한 메시지)");continue;
                }
                if(c.checkSelfPermission(Manifest.permission.SEND_SMS)!=PackageManager.PERMISSION_GRANTED || !SubscriptionManager.isValidSubscriptionId(sub)) {
                    store.state(id,"FAILED","SMS 권한 또는 선택한 SIM 확인 필요",0);update(c,"자동발송 실패: 권한/SIM 확인 필요");continue;
                }
                ArrayList<PendingIntent> callbacks=new ArrayList<>();
                try {
                    SmsManager manager=c.getSystemService(SmsManager.class).createForSubscriptionId(sub);
                    ArrayList<String> parts=manager.divideMessage(OutgoingMessage.format(body));
                    for(int p=0;p<parts.size();p++) {
                        Intent intent=new Intent(c,AutoResultReceiver.class).setAction(c.getPackageName()+".AUTO_SENT")
                            .setData(Uri.parse("auto-sms:"+id+":"+p)).putExtra("id",id).putExtra("part",p);
                        callbacks.add(PendingIntent.getBroadcast(c,0,intent,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_ONE_SHOT));
                    }
                    if(!enabled(c)||!AutoService.running){ for(PendingIntent pi:callbacks)pi.cancel();store.state(id,"CANCELLED","사용자가 자동발송을 중지함",parts.size());return; }
                    store.state(id,"PENDING","발송 요청. 결과 대기 중",parts.size());
                    if(parts.size()==1)manager.sendTextMessage(target.getKey(),null,parts.get(0),callbacks.get(0),null);
                    else manager.sendMultipartTextMessage(target.getKey(),null,parts,callbacks,null);
                    update(c,"자동발송 요청 완료 · 발송 내역에서 결과 확인");
                } catch(RuntimeException e) {
                    for(PendingIntent pi:callbacks)pi.cancel();
                    store.state(id,"UNKNOWN","발송 요청 오류: "+e.getClass().getSimpleName()+" · 수신 확인 필요, 자동 재시도 없음",-1);
                    update(c,"자동발송 요청 오류 · 발송 내역 확인");
                }
            }
        }
    }
}
