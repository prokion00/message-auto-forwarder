package com.personal.messageforwarder;
import android.app.Notification;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import java.util.LinkedHashMap;
import java.util.Map;

public final class KakaoNotificationService extends NotificationListenerService {
    private final Map<String,Long> recent=new LinkedHashMap<String,Long>(){protected boolean removeEldestEntry(Map.Entry<String,Long> e){return size()>50;}};
    @Override public void onNotificationPosted(StatusBarNotification sbn){
        if(!"com.kakao.talk".equals(sbn.getPackageName()))return;Bundle e=sbn.getNotification().extras;
        String sender=str(e.getCharSequence(Notification.EXTRA_TITLE));String body=str(e.getCharSequence(Notification.EXTRA_BIG_TEXT));if(body.isEmpty())body=str(e.getCharSequence(Notification.EXTRA_TEXT));if(body.isEmpty())return;
        String key=sender+"\n"+body;long now=System.currentTimeMillis();Long before=recent.get(key);if(before!=null&&now-before<5000)return;recent.put(key,now);Forwarder.process(this,"KAKAO",sender,body);
    }
    private String str(CharSequence s){return s==null?"":s.toString().trim();}
}
