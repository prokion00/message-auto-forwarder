package com.personal.messageforwarder;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.telephony.SmsManager;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class Forwarder {
    public static void process(Context c,String source,String sender,String body){
        if(body==null||body.trim().isEmpty())return;
        for(Rule r:RuleStore.load(c)){if(!matches(r,source,sender,body))continue;
            HistoryItem h=new HistoryItem();h.id=UUID.randomUUID().toString();h.createdAt=h.updatedAt=System.currentTimeMillis();
            h.ruleName=r.name;h.source=source;h.sender=sender;h.destination=phone(r.destination);h.body=body;
            try{
                SmsManager sm=SmsManager.getDefault();
                ArrayList<String> parts=sm.divideMessage(body);h.totalParts=parts.size();h.status="QUEUED";h.detail="규칙 일치, SMS 발송 요청";HistoryStore.add(c,h);
                ArrayList<PendingIntent> sent=new ArrayList<>(),delivered=new ArrayList<>();
                for(int part=0;part<parts.size();part++){
                    sent.add(resultIntent(c,h.id,part,true));delivered.add(resultIntent(c,h.id,part,false));
                }
                sm.sendMultipartTextMessage(h.destination,null,parts,sent,delivered);
            }catch(Exception e){
                if(h.createdAt>0){if(HistoryStore.load(c).stream().noneMatch(x->x.id.equals(h.id)))HistoryStore.add(c,h);HistoryStore.updateResult(c,h.id,"SENT",false,e.getClass().getSimpleName()+": "+String.valueOf(e.getMessage()));}
                Log.e("MessageForwarder","전달 실패: "+r.name,e);
            }
        }
    }

    private static PendingIntent resultIntent(Context c,String id,int part,boolean sent){
        Intent i=new Intent(c,SmsResultReceiver.class);i.setAction(sent?SmsResultReceiver.ACTION_SENT:SmsResultReceiver.ACTION_DELIVERED);i.putExtra("historyId",id);
        int request=(id+(sent?"S":"D")+part).hashCode();return PendingIntent.getBroadcast(c,request,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    }
    static boolean matches(Rule r,String source,String sender,String body){
        if(!r.enabled||r.destination.trim().isEmpty()||!(r.source.equals("BOTH")||r.source.equals(source)))return false;
        if(!r.senders.trim().isEmpty()&&!senderMatches(r.senders,sender))return false;
        String text=body.toLowerCase(Locale.ROOT);for(String s:tokens(r.exclude))if(text.contains(s))return false;
        for(String s:tokens(r.includeAll))if(!text.contains(s))return false;
        List<String> any=tokens(r.includeAny);if(!any.isEmpty()){boolean found=false;for(String s:any)if(text.contains(s)){found=true;break;}if(!found)return false;}return true;
    }
    private static boolean senderMatches(String patterns,String sender){String n=phone(sender);for(String raw:patterns.split("[,\n]")){String p=phone(raw.trim());if(p.isEmpty())continue;if(raw.trim().endsWith("*")&&n.startsWith(p.replace("*","")))return true;if(n.equals(p)||sender.equalsIgnoreCase(raw.trim()))return true;}return false;}
    private static List<String> tokens(String value){List<String> out=new ArrayList<>();for(String s:value.split("[,\n]"))if(!s.trim().isEmpty())out.add(s.trim().toLowerCase(Locale.ROOT));return out;}
    private static String phone(String s){return s==null?"":s.replaceAll("[^0-9+*]","");}
}
