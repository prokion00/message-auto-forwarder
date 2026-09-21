package com.personal.messageforwarder.diagnostics;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.SharedPreferences;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.provider.Telephony;
import android.telephony.SmsManager;
import android.telephony.SubscriptionManager;
import android.text.InputType;
import android.widget.EditText;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class MainActivity extends Activity {
    private final String[] permissions = {Manifest.permission.SEND_SMS,
        Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS};
    private final String[] labels = {"SMS 발송", "SMS 수신", "SMS/MMS 읽기"};
    private Button autoButton;
    private final SharedPreferences.OnSharedPreferenceChangeListener autoListener=(p,key)->runOnUiThread(this::refresh);
    private TextView received;
    private java.util.concurrent.ScheduledExecutorService scanner;
    private final SharedPreferences.OnSharedPreferenceChangeListener receiveListener = (p, key) -> runOnUiThread(this::refresh);
    private TextView status;
    private TextView sendStatus;
    private EditText destination;
    private Button send;
    private SharedPreferences prefs;
    private static final String TEST_BODY = OutgoingMessage.format("문자 자동전달 앱 발송 테스트입니다.");
    private final SharedPreferences.OnSharedPreferenceChangeListener listener = (p, key) -> runOnUiThread(this::refresh);
    private String lastResult = "아직 권한을 요청하지 않았습니다.";

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("test", MODE_PRIVATE);
        if (!ReceiveLog.prefs(this).contains("since")) ReceiveLog.prefs(this).edit().putLong("since", System.currentTimeMillis()).apply();
        if (state != null) lastResult = state.getString("result", lastResult);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = (int)(24 * getResources().getDisplayMetrics().density);
        layout.setPadding(pad, pad, pad, pad);
        layout.setOnApplyWindowInsetsListener((view, insets) -> {
            android.graphics.Insets bars = insets.getInsets(android.view.WindowInsets.Type.systemBars());
            view.setPadding(pad + bars.left, pad + bars.top, pad + bars.right, pad + bars.bottom);
            return insets;
        });
        TextView title = new TextView(this);
        title.setText("진단 및 테스트\n\n권한과 수신 내용을 확인하고 테스트 SMS 한 건을 보낼 수 있습니다.\n일상적인 사용에는 홈 화면의 자동전달 상태만 확인하면 됩니다.\n");
        title.setTextSize(20);
        layout.addView(title);
        autoButton=new Button(this);
        autoButton.setOnClickListener(v->toggleAutomatic());
        layout.addView(autoButton);
        Button historyButton=new Button(this);historyButton.setText("자동발송 내역");
        historyButton.setOnClickListener(v->startActivity(new Intent(this,AutoHistoryActivity.class)));layout.addView(historyButton);
        status = new TextView(this);
        status.setTextSize(17);
        layout.addView(status);
        Button request = new Button(this);
        request.setText("SMS 권한 요청");
        request.setOnClickListener(v -> requestPermissions(permissions, 10));
        layout.addView(request);
        Button refresh = new Button(this);
        refresh.setText("상태 새로고침");
        refresh.setOnClickListener(v -> refresh());
        layout.addView(refresh);
        Button settings = new Button(this);
        settings.setText("이 앱의 설정 열기");
        settings.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:" + getPackageName()))));
        layout.addView(settings);
        Button home = new Button(this);
        home.setText("홈으로 돌아가기");
        home.setOnClickListener(v -> finish());
        layout.addView(home);
        Button notificationAccess = new Button(this);
        notificationAccess.setText("RCS·카카오톡 알림 접근 허용");
        notificationAccess.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)));
        layout.addView(notificationAccess);
        TextView explanation = new TextView(this);
        explanation.setText("수신 기록 · 최근 50건 기기 내 보관\n자동발송을 켜면 화면 밖에서도 SMS·MMS를 감시합니다.\n자동발송이 켜진 상태로 재부팅하면 감시 서비스가 자동 복구됩니다.\n알림이 없거나 내용이 숨겨지면 RCS·카카오톡을 읽지 못할 수 있습니다.\nRCS는 메시지 알림으로 표시합니다(SMS/MMS 알림도 포함될 수 있음).\n번호가 제공되지 않으면 규칙에 알림의 발신인명을 등록하세요.\n");
        layout.addView(explanation);
        Button clear = new Button(this);
        clear.setText("수신 진단 기록 지우기");
        clear.setOnClickListener(v -> ReceiveLog.prefs(this).edit().remove("events").putLong("since", System.currentTimeMillis()).apply());
        layout.addView(clear);
        received = new TextView(this);
        received.setTextSize(16);
        received.setTextIsSelectable(true);
        layout.addView(received);
        destination = new EditText(this);
        destination.setId(R.id.destination);
        destination.setHint("테스트 수신 휴대폰 번호 (010…)");
        destination.setInputType(InputType.TYPE_CLASS_PHONE);
        layout.addView(destination);
        TextView body = new TextView(this);
        body.setText("보낼 내용: " + TEST_BODY + "\n휴대폰의 기본 SMS SIM으로 전송합니다. 요금제에 따라 문자 요금이 발생할 수 있습니다.");
        layout.addView(body);
        send = new Button(this);
        send.setText("테스트 문자 1건 보내기");
        send.setOnClickListener(v -> confirmSend());
        layout.addView(send);
        sendStatus = new TextView(this);
        sendStatus.setTextSize(18);
        layout.addView(sendStatus);
        ScrollView scroll = new ScrollView(this);
        scroll.addView(layout);
        setContentView(scroll);
    }

    @Override public void onResume() { super.onResume(); refresh(); }
    @Override public void onStart() {
        super.onStart();
        prefs.registerOnSharedPreferenceChangeListener(listener);
        ReceiveLog.prefs(this).registerOnSharedPreferenceChangeListener(receiveListener);
        AutoForwarder.prefs(this).registerOnSharedPreferenceChangeListener(autoListener);
        scanner = java.util.concurrent.Executors.newSingleThreadScheduledExecutor();
        scanner.scheduleWithFixedDelay(() -> { if(!AutoService.running){ SmsScanner.scan(getApplicationContext()); MmsScanner.scan(getApplicationContext()); } }, 0, 3, java.util.concurrent.TimeUnit.SECONDS);
    }
    @Override public void onStop() {
        scanner.shutdownNow();
        prefs.unregisterOnSharedPreferenceChangeListener(listener);
        ReceiveLog.prefs(this).unregisterOnSharedPreferenceChangeListener(receiveListener);
        AutoForwarder.prefs(this).unregisterOnSharedPreferenceChangeListener(autoListener);
        super.onStop();
    }
    @Override public void onSaveInstanceState(Bundle out) {
        out.putString("result", lastResult);
        super.onSaveInstanceState(out);
    }
    @Override public void onRequestPermissionsResult(int code, String[] names, int[] grants) {
        super.onRequestPermissionsResult(code, names, grants);
        if(code==20){AutoForwarder.update(this,"알림 권한 확인 후 자동발송 켜기를 다시 눌러 주세요.");refresh();return;}
        lastResult = grants.length == 0 ? "권한 요청이 취소되었습니다." : "권한 요청 완료. 아래 실제 허용 상태를 확인하세요.";
        refresh();
    }
    private void refresh() {
        if (status == null) return;
        StringBuilder text = new StringBuilder("Android " + android.os.Build.VERSION.RELEASE
            + "\n기본 문자 앱: " + Telephony.Sms.getDefaultSmsPackage(this) + "\n\n");
        for (int i = 0; i < permissions.length; i++) {
            text.append(labels[i]).append(": ").append(checkSelfPermission(permissions[i])
                == PackageManager.PERMISSION_GRANTED ? "허용됨" : "허용되지 않음").append("\n");
        }
        text.append("\n").append(lastResult).append("\n\n허용되지 않은 원인은 기기 진단과 함께 확인합니다.\n");
        boolean access = getSystemService(android.app.NotificationManager.class).isNotificationListenerAccessGranted(
            new android.content.ComponentName(this, IncomingNotificationService.class));
        text.append("알림 접근: ").append(access ? "허용됨" : "허용 필요").append("\n");
        text.append(ReceiveLog.prefs(this).getString("smsScanStatus", "SMS 저장소 확인 대기")).append("\n");
        boolean auto=AutoForwarder.enabled(this);
        autoButton.setText(auto ? (AutoService.running?"자동발송 켜짐 · 눌러서 끄기":"자동발송 서비스 정지 · 눌러서 초기화") : "자동발송 꺼짐 · 눌러서 켜기");
        text.append("\n자동발송: ").append(auto&&AutoService.running?"켜짐":"꺼짐/정지").append("\n")
            .append(AutoForwarder.prefs(this).getString("last","자동발송을 켜면 새 수신 메시지만 처리합니다.")).append("\n");
        status.setText(text.toString());
        if (received != null) received.setText(ReceiveLog.display(this));
        if (sendStatus != null) {
            sendStatus.setText("\n발송 결과: " + prefs.getString("result", "아직 발송하지 않았습니다."));
            send.setEnabled(!prefs.getBoolean("pending", false));
        }
    }

    private void toggleAutomatic(){
        if(AutoForwarder.enabled(this)){
            AutoForwarder.prefs(this).edit().putBoolean("enabled",false).commit();
            stopService(new Intent(this,AutoService.class));AutoForwarder.update(this,"자동발송 꺼짐. 이미 요청한 문자는 취소되지 않습니다.");refresh();return;
        }
        for(String permission:permissions)if(checkSelfPermission(permission)!=PackageManager.PERMISSION_GRANTED){
            new AlertDialog.Builder(this).setMessage("먼저 SMS 읽기·수신·발송 권한을 허용해 주세요.").setPositiveButton("확인",null).show();return;
        }
        if(checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},20);return;
        }
        int sub=SubscriptionManager.getDefaultSmsSubscriptionId();
        if(!SubscriptionManager.isValidSubscriptionId(sub)){
            new AlertDialog.Builder(this).setMessage("SIM 관리자에서 기본 문자 발송 SIM을 지정해 주세요.").setPositiveButton("확인",null).show();return;
        }
        org.json.JSONArray rules=Rules.load(this);int count=0;StringBuilder summary=new StringBuilder();
        for(int i=0;i<rules.length();i++){
            org.json.JSONObject r=rules.optJSONObject(i);if(r==null||!r.optBoolean("enabled",true))continue;
            if(RuleMatcher.terms(r.optString("senders")).isEmpty()||RuleMatcher.terms(r.optString("destinations")).isEmpty())continue;
            count++;summary.append("\n").append(r.optString("name")).append(" → ").append(r.optString("destinations"));
        }
        if(count==0){new AlertDialog.Builder(this).setMessage("사용할 규칙을 1개 이상 등록해 주세요.").setPositiveButton("확인",null).show();return;}
        new AlertDialog.Builder(this).setTitle("자동발송 켜기")
            .setMessage("켜는 시점 이후 조건에 맞는 새 메시지를 아래 번호로 보냅니다.\n[자동전송] 표시를 붙이며 긴 본문은 여러 SMS로 나뉩니다.\n동일 본문·대상은 2분 동안 중복 차단합니다.\n실패·미확인은 자동 재시도하지 않습니다. 문자 요금이 발생할 수 있습니다.\n"+summary)
            .setNegativeButton("취소",null).setPositiveButton("자동발송 켜기",(d,w)->{
                boolean saved=AutoForwarder.prefs(this).edit().putBoolean("enabled",true).putLong("enabledAt",System.currentTimeMillis()).putInt("subscription",sub).commit();
                if(!saved){AutoForwarder.update(this,"설정 저장 실패");return;}
                try{startForegroundService(new Intent(this,AutoService.class));}
                catch(RuntimeException e){AutoForwarder.prefs(this).edit().putBoolean("enabled",false).commit();AutoForwarder.update(this,"자동발송 시작 실패: "+e.getClass().getSimpleName());}
                refresh();
            }).show();
    }
    private void confirmSend() {
        if (checkSelfPermission(Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            new AlertDialog.Builder(this).setMessage("먼저 SMS 발송 권한을 허용해 주세요.").setPositiveButton("확인", null).show();
            return;
        }
        String number = destination.getText().toString().replaceAll("[\\s()-]", "");
        if (!number.matches("010[0-9]{8}")) {
            destination.setError("테스트는 국내 휴대폰 번호 010으로 시작하는 11자리로 입력해 주세요.");
            return;
        }
        int subscription = SubscriptionManager.getDefaultSmsSubscriptionId();
        if (!SubscriptionManager.isValidSubscriptionId(subscription)) {
            new AlertDialog.Builder(this).setMessage("기본 SMS SIM이 정해져 있지 않습니다. 휴대폰 설정의 SIM 관리자에서 문자용 SIM을 선택한 뒤 다시 시도해 주세요.").setPositiveButton("확인", null).show();
            return;
        }
        new AlertDialog.Builder(this).setTitle("테스트 SMS 발송 확인")
            .setMessage("받는 번호: " + number + "\n\n" + TEST_BODY + "\n\n이 번호로 SMS 한 건을 보냅니다.")
            .setNegativeButton("취소", null)
            .setPositiveButton("1건 발송", (dialog, which) -> sendTest(number, subscription)).show();
    }

    private void sendTest(String number, int subscription) {
        if (prefs.getBoolean("pending", false)) return;
        String id = java.util.UUID.randomUUID().toString();
        Intent callback = new Intent(this, SendResultReceiver.class)
            .setAction(getPackageName() + ".SENT")
            .setData(Uri.parse("sms-test:" + id)).putExtra("id", id);
        PendingIntent sent = PendingIntent.getBroadcast(this, 0, callback,
            PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);
        // Persist before handing off: recreation must not submit a duplicate SMS.
        boolean saved = prefs.edit().putString("id", id).putBoolean("pending", true)
            .putString("result", "발송 결과 대기 중… 자동 재발송하지 않습니다. 결과가 계속 없으면 PC에서 진단해 주세요.").commit();
        if (!saved) {
            sent.cancel();
            new AlertDialog.Builder(this).setMessage("발송 상태를 저장할 수 없어 전송하지 않았습니다.").setPositiveButton("확인", null).show();
            return;
        }
        refresh();
        try {
            SmsManager manager = getSystemService(SmsManager.class).createForSubscriptionId(subscription);
            if (manager.divideMessage(TEST_BODY).size() != 1) throw new IllegalArgumentException("본문 분할 필요");
            manager.sendTextMessage(number, null, TEST_BODY, sent, null);
        } catch (RuntimeException e) {
            sent.cancel();
            prefs.edit().putBoolean("pending", false)
                .putString("result", "발송 요청 오류: " + e.getClass().getSimpleName() + ". 수신 여부를 확인한 뒤 다시 시도해 주세요.").apply();
        }
    }
}







