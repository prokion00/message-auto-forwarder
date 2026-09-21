package com.personal.messageforwarder.diagnostics;

import android.app.*;
import android.content.*;
import android.content.pm.ServiceInfo;
import android.database.ContentObserver;
import android.os.*;
import android.provider.Telephony;
import java.util.concurrent.*;

public class AutoService extends Service {
    static final String ACTION_RESTORE="com.personal.messageforwarder.diagnostics.RESTORE_AUTOMATIC";
    static final String EXTRA_RESTORE_REASON="restoreReason";
    static volatile boolean running=false;
    private ScheduledExecutorService scanner;
    private ContentObserver observer;
    @Override public void onCreate(){
        super.onCreate();
        NotificationManager nm=getSystemService(NotificationManager.class);
        nm.createNotificationChannel(new NotificationChannel("automatic","문자 자동발송 상태",NotificationManager.IMPORTANCE_LOW));
    }
    @Override public int onStartCommand(Intent intent,int flags,int startId){
        boolean restoring=intent!=null && ACTION_RESTORE.equals(intent.getAction());
        String restoreReason=restoring?intent.getStringExtra(EXTRA_RESTORE_REASON):null;
        if(intent!=null && "STOP".equals(intent.getAction())) {
            AutoForwarder.prefs(this).edit().putBoolean("enabled",false).commit();
            AutoForwarder.update(this,"자동발송을 중지했습니다.");stopSelf();return START_NOT_STICKY;
        }
        if(!AutoForwarder.enabled(this)){stopSelf();return START_NOT_STICKY;}
        PendingIntent open=PendingIntent.getActivity(this,0,new Intent(this,HomeActivity.class),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent stop=PendingIntent.getService(this,0,new Intent(this,AutoService.class).setAction("STOP"),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        Notification notification=new Notification.Builder(this,"automatic").setSmallIcon(android.R.drawable.ic_dialog_email)
            .setContentTitle("문자자동전달 켜짐").setContentText("조건에 맞는 새 메시지를 기다리고 있습니다.")
            .setContentIntent(open).setOngoing(true).addAction(new Notification.Action.Builder(null,"자동발송 중지",stop).build()).build();
        try { startForeground(800,notification,ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE); }
        catch(RuntimeException e){ AutoForwarder.prefs(this).edit().putBoolean("enabled",false).commit();AutoForwarder.update(this,"백그라운드 시작 실패: "+e.getClass().getSimpleName());stopSelf();return START_NOT_STICKY; }
        if(!running) {
            running=true;
            scanner=Executors.newSingleThreadScheduledExecutor();
            Runnable scan=()->{ try {SmsScanner.scan(getApplicationContext());MmsScanner.scan(getApplicationContext());}
                catch(RuntimeException e){AutoForwarder.update(this,"수신 조회 오류: "+e.getClass().getSimpleName());} };
            observer=new ContentObserver(new Handler(Looper.getMainLooper())){
                @Override public void onChange(boolean self){ if(scanner!=null&&!scanner.isShutdown())scanner.execute(scan); }
            };
            try {
                getContentResolver().registerContentObserver(Telephony.Sms.CONTENT_URI,true,observer);
                getContentResolver().registerContentObserver(Telephony.Mms.CONTENT_URI,true,observer);
            } catch(SecurityException e){AutoForwarder.update(this,"문자 읽기 권한을 확인해 주세요.");}
            scanner.scheduleWithFixedDelay(scan,0,15,TimeUnit.SECONDS);
            AutoForwarder.update(this,"자동발송 대기 중");
        }
        if(restoring) AutoForwarder.update(this,(restoreReason==null?"시스템 시작":restoreReason)+" 후 자동발송 복구 완료");
        return START_STICKY;
    }
    @Override public void onDestroy(){
        running=false;
        if(observer!=null)getContentResolver().unregisterContentObserver(observer);
        if(scanner!=null)scanner.shutdownNow();
        stopForeground(STOP_FOREGROUND_REMOVE);
        AutoForwarder.update(this,AutoForwarder.enabled(this)?"감시 서비스 종료 · 앱에서 상태 확인 필요":"자동발송 꺼짐");
        super.onDestroy();
    }
    @Override public IBinder onBind(Intent intent){return null;}
}
