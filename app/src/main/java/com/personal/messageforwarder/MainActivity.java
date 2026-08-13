package com.personal.messageforwarder;
import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class MainActivity extends Activity {
    private LinearLayout list;private List<Rule> rules=new ArrayList<>();
    @Override public void onCreate(Bundle b){super.onCreate(b);build();requestPermissions(new String[]{Manifest.permission.RECEIVE_SMS,Manifest.permission.SEND_SMS},10);}
    @Override protected void onResume(){super.onResume();rules=RuleStore.load(this);render();}
    private void build(){LinearLayout root=box(LinearLayout.VERTICAL);root.setPadding(dp(16),dp(12),dp(16),dp(20));TextView title=text("문자 자동전달",26);title.setTextColor(Color.rgb(25,103,210));root.addView(title);root.addView(text("SMS와 카카오톡 알림을 규칙에 따라 지정 번호로 원문 그대로 전달합니다.",15));Button permission=button("SMS 권한 허용");permission.setOnClickListener(v->requestPermissions(new String[]{Manifest.permission.RECEIVE_SMS,Manifest.permission.SEND_SMS},10));root.addView(permission);Button access=button("카카오톡 알림 접근 설정 열기");access.setOnClickListener(v->startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)));root.addView(access);Button history=button("전달 History 보기");history.setOnClickListener(v->showHistory());root.addView(history);Button add=button("+ 새 전달 규칙");add.setOnClickListener(v->edit(null));root.addView(add);ScrollView scroll=new ScrollView(this);list=box(LinearLayout.VERTICAL);scroll.addView(list);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);}
    private void render(){if(list==null)return;list.removeAllViews();if(rules.isEmpty())list.addView(text("아직 규칙이 없습니다.",16));for(Rule r:rules){LinearLayout card=box(LinearLayout.VERTICAL);card.setPadding(dp(10),dp(8),dp(10),dp(8));card.addView(text((r.enabled?"● ":"○ ")+r.name,19));card.addView(text(label(r.source)+" → "+r.destination+"\n발신: "+empty(r.senders)+"\nOR: "+empty(r.includeAny)+" | AND: "+empty(r.includeAll)+" | 제외: "+empty(r.exclude),14));LinearLayout buttons=box(LinearLayout.HORIZONTAL);Button edit=button("수정");edit.setOnClickListener(v->edit(r));buttons.addView(edit,new LinearLayout.LayoutParams(0,-2,1));Button toggle=button(r.enabled?"끄기":"켜기");toggle.setOnClickListener(v->{r.enabled=!r.enabled;save();});buttons.addView(toggle,new LinearLayout.LayoutParams(0,-2,1));Button del=button("삭제");del.setOnClickListener(v->new AlertDialog.Builder(this).setMessage("이 규칙을 삭제할까요?").setPositiveButton("삭제",(d,w)->{rules.remove(r);save();}).setNegativeButton("취소",null).show());buttons.addView(del,new LinearLayout.LayoutParams(0,-2,1));card.addView(buttons);list.addView(card);}}
    private void edit(Rule existing){Rule r=existing==null?new Rule():existing;LinearLayout form=box(LinearLayout.VERTICAL);form.setPadding(dp(18),0,dp(18),0);EditText name=field(form,"규칙 이름",r.name);form.addView(text("적용 대상",14));Spinner source=new Spinner(this);String[] choices={"문자 + 카카오톡","문자만","카카오톡만"};source.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,choices));source.setSelection(r.source.equals("SMS")?1:r.source.equals("KAKAO")?2:0);form.addView(source);EditText dest=field(form,"전달받을 번호 (필수)",r.destination);EditText senders=field(form,"발신자/번호 (쉼표 구분, 예: 01012*)",r.senders);EditText any=field(form,"하나 이상 포함 OR (쉼표 구분)",r.includeAny);EditText all=field(form,"모두 포함 AND (쉼표 구분)",r.includeAll);EditText exclude=field(form,"제외 문자열 (쉼표 구분)",r.exclude);AlertDialog dialog=new AlertDialog.Builder(this).setTitle(existing==null?"새 규칙":"규칙 수정").setView(form).setPositiveButton("저장",null).setNegativeButton("취소",null).create();dialog.setOnShowListener(d->dialog.getButton(-1).setOnClickListener(v->{if(dest.getText().toString().trim().isEmpty()){dest.setError("번호를 입력하세요");return;}r.name=name.getText().toString().trim();r.destination=dest.getText().toString().trim();r.senders=senders.getText().toString();r.includeAny=any.getText().toString();r.includeAll=all.getText().toString();r.exclude=exclude.getText().toString();r.source=source.getSelectedItemPosition()==1?"SMS":source.getSelectedItemPosition()==2?"KAKAO":"BOTH";if(existing==null)rules.add(r);save();dialog.dismiss();}));dialog.show();}

    private void showHistory(){
        List<HistoryItem> items=HistoryStore.load(this);LinearLayout content=box(LinearLayout.VERTICAL);content.setPadding(dp(12),0,dp(12),dp(12));
        if(items.isEmpty())content.addView(text("전달 기록이 없습니다. 규칙과 일치한 메시지가 생기면 여기에 표시됩니다.",15));
        SimpleDateFormat format=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",Locale.KOREA);
        for(HistoryItem h:items){
            TextView row=text(statusLabel(h.status)+"  "+format.format(new Date(h.updatedAt))+"\n"+(h.source.equals("KAKAO")?"카카오톡":"문자")+" / "+h.ruleName+"\n"+h.sender+" → "+h.destination+"\n"+h.body+"\n"+h.detail,14);
            row.setPadding(dp(8),dp(8),dp(8),dp(12));content.addView(row);
        }
        ScrollView scroll=new ScrollView(this);scroll.addView(content);AlertDialog dialog=new AlertDialog.Builder(this).setTitle("전달 History (최대 200건)").setView(scroll).setPositiveButton("닫기",null).setNegativeButton("전체 삭제",null).create();
        dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(v->new AlertDialog.Builder(this).setMessage("모든 History를 삭제할까요?").setPositiveButton("삭제",(x,w)->{HistoryStore.clear(this);dialog.dismiss();}).setNegativeButton("취소",null).show()));dialog.show();
    }

    private String statusLabel(String status){switch(status){case "DELIVERED":return "✅ 전달 완료";case "SENT":return "✓ 발송 성공";case "SEND_FAILED":return "❌ 발송 실패";case "DELIVERY_FAILED":return "⚠ 전달 실패";default:return "⏳ 발송 중";}}
    private void save(){RuleStore.save(this,rules);render();}private EditText field(LinearLayout p,String hint,String value){p.addView(text(hint,14));EditText e=new EditText(this);e.setText(value);p.addView(e);return e;}private LinearLayout box(int o){LinearLayout l=new LinearLayout(this);l.setOrientation(o);return l;}private TextView text(String s,int z){TextView t=new TextView(this);t.setText(s);t.setTextSize(z);t.setPadding(0,dp(5),0,dp(5));return t;}private Button button(String s){Button b=new Button(this);b.setText(s);return b;}private int dp(int n){return(int)(n*getResources().getDisplayMetrics().density);}private String empty(String s){return s.trim().isEmpty()?"없음":s;}private String label(String s){return s.equals("SMS")?"문자":s.equals("KAKAO")?"카카오톡":"문자+카카오톡";}
}
