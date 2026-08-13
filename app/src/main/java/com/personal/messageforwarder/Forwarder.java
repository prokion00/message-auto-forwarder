package com.personal.messageforwarder;
import android.content.Context;
import android.telephony.SmsManager;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class Forwarder {
    public static void process(Context c,String source,String sender,String body){
        if(body==null||body.trim().isEmpty())return;
        for(Rule r:RuleStore.load(c)){if(!matches(r,source,sender,body))continue;try{
            SmsManager sm=SmsManager.getDefault();String label=source.equals("KAKAO")?"카카오톡":"문자";
            String msg="["+label+(sender.isEmpty()? "]" : " / "+sender+"]")+"\n"+body;
            ArrayList<String> parts=sm.divideMessage(msg);sm.sendMultipartTextMessage(phone(r.destination),null,parts,null,null);
        }catch(Exception e){Log.e("MessageForwarder","전달 실패: "+r.name,e);}}
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
