package com.personal.messageforwarder.diagnostics;

import android.app.Instrumentation;
import android.os.Bundle;
import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;

public class AutoTests extends Instrumentation {
    private int count;
    private void check(boolean ok){if(!ok)throw new AssertionError("case "+(count+1));count++;}
    @Override public void onCreate(Bundle args){super.onCreate(args);start();}
    @Override public void onStart(){
        Bundle result=new Bundle();String db="automatic-test-"+System.nanoTime()+".db";
        Context c=getTargetContext();
        try {
            check(!AutoPolicy.blocked("본문",99,100).isEmpty());
            check(!AutoPolicy.blocked("본문",100,100).isEmpty());
            check(AutoPolicy.blocked("본문",101,100).isEmpty());
            check(!AutoPolicy.blocked("  [자동전송] 원문",101,100).isEmpty());
            check(!AutoPolicy.blocked("",101,100).isEmpty());
            check(OutgoingMessage.format("원문\n다음 줄").equals("[자동전송] 원문\n다음 줄"));
            check(BootReceiver.isRestoreAction(android.content.Intent.ACTION_BOOT_COMPLETED));
            check(BootReceiver.isRestoreAction(android.content.Intent.ACTION_MY_PACKAGE_REPLACED));
            check(!BootReceiver.isRestoreAction(android.content.Intent.ACTION_SCREEN_ON));
            JSONObject event=new JSONObject().put("id","test").put("source","메시지 알림").put("sender","\u2068홍길동\u2069").put("address","01012345678").put("body","카드 1*8* 승인").put("eventAt",101);
            JSONObject rule=new JSONObject().put("name","A").put("enabled",true).put("channel","전체").put("senders","01012345678").put("all","1*8*").put("exclude","우리코치").put("destinations","01011112222,010-1111-2222");
            JSONArray rules=new JSONArray().put(rule).put(new JSONObject(rule.toString()).put("name","B"));
            check(AutoForwarder.targets(rules,event).size()==1);
            check(AutoForwarder.targets(rules,new JSONObject(event.toString()).put("body","1*8* 우리코치")).isEmpty());
            check(AutoForwarder.targets(rules,new JSONObject(event.toString()).put("uncertain",true)).isEmpty());
            check(AutoForwarder.targets(rules,new JSONObject(event.toString()).put("address","")).isEmpty());
            check(AutoForwarder.targets(new JSONArray().put(new JSONObject(rule.toString()).put("enabled",false)),event).isEmpty());
            try(AutoStore store=new AutoStore(c,db)) {
                check(store.reserve("one","event1","01011112222","fp",1000000,"SMS","A"));
                check(!store.reserve("two","event1","01011112222","different",1500000,"SMS","A"));
                check(!store.reserve("two","event2","01011112222","fp",1000100,"메시지 알림","A"));
                check(store.reserve("three","event3","01011112222","fp",1120001,"SMS","A"));
                check(store.reserve("four","event1","01033334444","fp2",1000000,"SMS","A"));
                store.state("one","PENDING","",2);
                store.callback("one",1,-1);check(store.status("one").equals("PENDING"));
                store.callback("one",1,-1);check(store.status("one").equals("PENDING"));
                store.callback("one",0,-1);check(store.status("one").equals("SENT"));
                store.state("three","PENDING","",2);
                store.callback("three",0,4);store.callback("three",1,-1);check(store.status("three").equals("FAILED"));
            }
            try(AutoStore store=new AutoStore(c,db)) {
                check(!store.reserve("restart","event1","01011112222","fp",9000000,"SMS","A"));
                check(!store.reserve("alias-replay","event2","01011112222","fp",9000000,"메시지 알림","A"));
                check(store.status("two").equals("DUPLICATE"));
                check(store.status("one").equals("SENT"));
            }
            result.putString("stream","PASS: "+count+" automatic forwarding checks; no SMS was sent.\n");finish(-1,result);
        } catch(Throwable e){result.putString("stream","FAIL: "+e.toString()+"\n");finish(0,result);}
        finally {c.deleteDatabase(db);}
    }
}

