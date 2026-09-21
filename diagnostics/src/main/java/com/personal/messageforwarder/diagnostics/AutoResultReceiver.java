package com.personal.messageforwarder.diagnostics;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
public class AutoResultReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c,Intent i){
        if(!(c.getPackageName()+".AUTO_SENT").equals(i.getAction()) || i.getStringExtra("id")==null)return;
        try(AutoStore store=new AutoStore(c)) { store.callback(i.getStringExtra("id"),i.getIntExtra("part",-1),getResultCode()); }
        AutoForwarder.update(c,"발송 결과 업데이트 · 발송 내역 확인");
    }
}
