package com.personal.messageforwarder.diagnostics;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.telephony.SubscriptionManager;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;

public final class HomeActivity extends Activity {
    private TextView stateTitle,stateDetail,ruleSummary;
    private LinearLayout stateCard;
    private Button automatic;
    private final android.content.SharedPreferences.OnSharedPreferenceChangeListener listener=(p,k)->runOnUiThread(this::refresh);

    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        LinearLayout root=AppUi.page(this);
        root.addView(AppUi.title(this,"문자자동전달"));
        TextView subtitle=AppUi.text(this,"필요한 메시지만 골라 안전하게 전달합니다.",16,AppUi.MUTED);
        subtitle.setPadding(0,AppUi.dp(this,5),0,0);root.addView(subtitle);
        AppUi.gap(this,root,24);

        stateCard=new LinearLayout(this);stateCard.setOrientation(LinearLayout.VERTICAL);int cp=AppUi.dp(this,20);stateCard.setPadding(cp,cp,cp,cp);
        stateCard.setElevation(AppUi.dp(this,2));
        TextView label=AppUi.text(this,"자동전달 상태",14,AppUi.MUTED);stateCard.addView(label);
        stateTitle=AppUi.text(this,"확인 중",25,AppUi.INK);stateTitle.setTypeface(null,android.graphics.Typeface.BOLD);stateTitle.setPadding(0,AppUi.dp(this,7),0,0);stateCard.addView(stateTitle);
        stateDetail=AppUi.text(this,"",15,AppUi.MUTED);stateDetail.setPadding(0,AppUi.dp(this,8),0,0);stateCard.addView(stateDetail);
        ruleSummary=AppUi.text(this,"",14,AppUi.MUTED);ruleSummary.setPadding(0,AppUi.dp(this,12),0,0);stateCard.addView(ruleSummary);
        root.addView(stateCard,new LinearLayout.LayoutParams(-1,-2));
        AppUi.gap(this,root,14);

        automatic=AppUi.button(this,"자동전달 켜기",true);automatic.setOnClickListener(v->toggleAutomatic());root.addView(automatic,new LinearLayout.LayoutParams(-1,-2));
        AppUi.gap(this,root,24);
        TextView manage=AppUi.text(this,"관리",15,AppUi.MUTED);manage.setTypeface(null,android.graphics.Typeface.BOLD);root.addView(manage);
        AppUi.gap(this,root,9);

        LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);
        Button rules=AppUi.button(this,"발송 규칙",false);rules.setOnClickListener(v->startActivity(new Intent(this,RulesActivity.class)));
        Button history=AppUi.button(this,"발송 내역",false);history.setOnClickListener(v->startActivity(new Intent(this,AutoHistoryActivity.class)));
        row.addView(rules,new LinearLayout.LayoutParams(0,-2,1));Space between=new Space(this);row.addView(between,new LinearLayout.LayoutParams(AppUi.dp(this,10),1));row.addView(history,new LinearLayout.LayoutParams(0,-2,1));root.addView(row);
        AppUi.gap(this,root,12);
        Button diagnostics=AppUi.button(this,"진단 및 테스트",false);diagnostics.setOnClickListener(v->startActivity(new Intent(this,MainActivity.class)));root.addView(diagnostics,new LinearLayout.LayoutParams(-1,-2));
        AppUi.gap(this,root,24);
        TextView note=AppUi.text(this,"자동전달이 켜져 있으면 화면이 꺼지거나 휴대폰을 재부팅해도 감시를 계속합니다.",14,AppUi.MUTED);root.addView(note);
        AppUi.showScrollable(this,root);
    }

    @Override public void onStart(){super.onStart();AutoForwarder.prefs(this).registerOnSharedPreferenceChangeListener(listener);}
    @Override public void onStop(){AutoForwarder.prefs(this).unregisterOnSharedPreferenceChangeListener(listener);super.onStop();}
    @Override public void onResume(){super.onResume();refresh();}
    @Override public void onRequestPermissionsResult(int request,String[] names,int[] results){
        super.onRequestPermissionsResult(request,names,results);
        if(request==31 && results.length>0 && results[0]==PackageManager.PERMISSION_GRANTED)toggleAutomatic();else refresh();
    }

    private void refresh(){
        if(stateTitle==null)return;
        boolean enabled=AutoForwarder.enabled(this),running=AutoService.running;
        int activeRules=activeRules();
        int tint;
        if(enabled&&running){stateTitle.setText("켜짐");stateDetail.setText("새 메시지를 기다리고 있습니다.");tint=Color.rgb(223,241,237);automatic.setText("자동전달 끄기");automatic.setTextColor(AppUi.INK);automatic.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(231,239,237)));}
        else if(enabled){stateTitle.setText("확인 필요");stateDetail.setText("서비스가 멈췄습니다. 아래 버튼으로 다시 시작해 주세요.");tint=Color.rgb(255,240,219);automatic.setText("자동전달 다시 시작");automatic.setTextColor(Color.WHITE);automatic.setBackgroundTintList(android.content.res.ColorStateList.valueOf(AppUi.PRIMARY));}
        else{stateTitle.setText("꺼짐");stateDetail.setText("조건에 맞는 메시지도 전달하지 않습니다.");tint=Color.WHITE;automatic.setText("자동전달 켜기");automatic.setTextColor(Color.WHITE);automatic.setBackgroundTintList(android.content.res.ColorStateList.valueOf(AppUi.PRIMARY));}
        stateCard.setBackground(AppUi.panel(tint,Color.rgb(220,230,227),AppUi.dp(this,18)));
        long changed=AutoForwarder.prefs(this).getLong("changed",0);
        String when=changed==0?"": " · "+DateFormat.format("MM-dd HH:mm",changed);
        ruleSummary.setText("사용 중인 규칙 "+activeRules+"개"+when+"\n"+AutoForwarder.prefs(this).getString("last","아직 자동전달 기록이 없습니다."));
    }

    private int activeRules(){
        JSONArray items=Rules.load(this);int count=0;
        for(int i=0;i<items.length();i++){JSONObject r=items.optJSONObject(i);if(r!=null&&r.optBoolean("enabled",true)&&!RuleMatcher.terms(r.optString("senders")).isEmpty()&&!RuleMatcher.terms(r.optString("destinations")).isEmpty())count++;}
        return count;
    }

    private void toggleAutomatic(){
        if(AutoForwarder.enabled(this)){
            if(!AutoService.running){
                try{startForegroundService(new Intent(this,AutoService.class));AutoForwarder.update(this,"자동전달 서비스를 다시 시작했습니다.");}
                catch(RuntimeException e){AutoForwarder.prefs(this).edit().putBoolean("enabled",false).commit();AutoForwarder.update(this,"자동전달 시작 실패: "+e.getClass().getSimpleName());}
                refresh();return;
            }
            new AlertDialog.Builder(this).setTitle("자동전달을 끌까요?").setMessage("새로 받는 메시지를 더 이상 전달하지 않습니다. 이미 발송을 요청한 문자는 취소되지 않습니다.")
                .setNegativeButton("취소",null).setPositiveButton("끄기",(d,w)->{AutoForwarder.prefs(this).edit().putBoolean("enabled",false).commit();stopService(new Intent(this,AutoService.class));AutoForwarder.update(this,"자동전달을 껐습니다.");refresh();}).show();return;
        }
        String[] required={Manifest.permission.SEND_SMS,Manifest.permission.RECEIVE_SMS,Manifest.permission.READ_SMS};
        for(String permission:required)if(checkSelfPermission(permission)!=PackageManager.PERMISSION_GRANTED){
            new AlertDialog.Builder(this).setTitle("SMS 권한이 필요합니다").setMessage("진단 및 테스트 화면에서 SMS 읽기·수신·발송 권한을 허용해 주세요.")
                .setNegativeButton("취소",null).setPositiveButton("진단 화면 열기",(d,w)->startActivity(new Intent(this,MainActivity.class))).show();return;
        }
        if(checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},31);return;}
        int sub=SubscriptionManager.getDefaultSmsSubscriptionId();
        if(!SubscriptionManager.isValidSubscriptionId(sub)){new AlertDialog.Builder(this).setMessage("휴대폰 설정의 SIM 관리자에서 문자 발송용 SIM을 지정해 주세요.").setPositiveButton("확인",null).show();return;}
        JSONArray items=Rules.load(this);StringBuilder summary=new StringBuilder();int count=0;
        for(int i=0;i<items.length();i++){JSONObject r=items.optJSONObject(i);if(r==null||!r.optBoolean("enabled",true)||RuleMatcher.terms(r.optString("senders")).isEmpty()||RuleMatcher.terms(r.optString("destinations")).isEmpty())continue;count++;summary.append("\n• ").append(r.optString("name")).append(" → ").append(r.optString("destinations").replace("\n",", "));}
        if(count==0){new AlertDialog.Builder(this).setMessage("사용할 발송 규칙을 먼저 등록해 주세요.").setNegativeButton("취소",null).setPositiveButton("규칙 설정",(d,w)->startActivity(new Intent(this,RulesActivity.class))).show();return;}
        new AlertDialog.Builder(this).setTitle("자동전달을 켤까요?").setMessage("이제부터 조건에 맞는 새 메시지를 전달합니다.\n전달 본문 앞에는 [자동전송]이 붙습니다."+summary)
            .setNegativeButton("취소",null).setPositiveButton("켜기",(d,w)->{
                boolean saved=AutoForwarder.prefs(this).edit().putBoolean("enabled",true).putLong("enabledAt",System.currentTimeMillis()).putInt("subscription",sub).commit();
                if(!saved){AutoForwarder.update(this,"설정 저장 실패");return;}
                try{startForegroundService(new Intent(this,AutoService.class));}catch(RuntimeException e){AutoForwarder.prefs(this).edit().putBoolean("enabled",false).commit();AutoForwarder.update(this,"자동전달 시작 실패: "+e.getClass().getSimpleName());}refresh();
            }).show();
    }
}
