package com.personal.messageforwarder.diagnostics;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.LinkedHashSet;

public class RulesActivity extends Activity {
    private LinearLayout layout;
    private EditText name, senders, all, any, exclude, destinations;
    private Spinner channel;
    private CheckBox enabled;
    private String editing;
    private static final String[] CHANNELS = {"전체", "SMS", "MMS", "카카오톡", "메시지 알림"};

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        if (state != null && state.containsKey("draft")) {
            try { editor(new JSONObject(state.getString("draft"))); } catch (Exception e) { list(); }
        } else list();
    }
    private void page(String title) {
        layout = AppUi.page(this);
        AppUi.showScrollable(this,layout);
        label(title, 23);
        AppUi.gap(this,layout,12);
    }
    private void label(String text, int size) {
        TextView view = AppUi.text(this,text,size,size>=22?AppUi.INK:AppUi.MUTED);if(size>=22)view.setTypeface(null,android.graphics.Typeface.BOLD);layout.addView(view);
    }
    private void button(String text, Runnable action) {
        Button b = AppUi.button(this,text,text.contains("저장")||text.contains("추가")); b.setOnClickListener(v -> action.run()); layout.addView(b);AppUi.gap(this,layout,7);
    }
    private EditText field(String label, String value) {
        label(label, 16);
        EditText e = new EditText(this); e.setText(value); e.setSingleLine(false); layout.addView(e); return e;
    }
    private void list() {
        editing = null;
        page("3단계 발송 규칙");
        label("자동발송이 켜져 있으면 저장한 규칙이 다음 새 메시지부터 적용됩니다.\n[자동전송] 표시 뒤에 받은 본문을 그대로 붙입니다.", 16);
        button("+ 규칙 추가", () -> editor(new JSONObject()));
        JSONArray items = Rules.load(this);
        for (int i = 0; i < items.length(); i++) {
            JSONObject r = items.optJSONObject(i); if (r == null) continue;
            button((r.optBoolean("enabled", true) ? "[켜짐] " : "[꺼짐] ") + r.optString("name") + " · " + r.optString("channel"), () -> editor(r));
        }
        button("돌아가기", this::finish);
    }
    private void editor(JSONObject r) {
        editing = r.optString("id", java.util.UUID.randomUUID().toString());
        page("규칙 등록·수정");
        name = field("규칙 이름", r.optString("name"));
        enabled = new CheckBox(this); enabled.setText("이 규칙 사용"); enabled.setChecked(r.optBoolean("enabled", true)); layout.addView(enabled);
        label("Step 1 · 발신인", 22);
        channel = new Spinner(this); channel.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, CHANNELS)); layout.addView(channel);
        for (int i = 0; i < CHANNELS.length; i++) if (CHANNELS[i].equals(r.optString("channel"))) channel.setSelection(i);
        senders = field("발신번호 또는 알림 발신인명\n쉼표/줄바꿈으로 여러 개 등록 · 하나라도 정확히 일치\nSMS·MMS는 번호, 카카오톡은 이름으로 판별합니다.\n메시지 알림은 RCS를 포함합니다. 번호가 없으면 표시된 이름을 등록하세요.", r.optString("senders"));
        label("Step 2 · 본문 조건", 22);
        label("각 칸은 쉼표/줄바꿈으로 구분합니다.\n포함 조건 칸들은 동시에 적용하며, 제외 조건을 우선합니다.\n빈 칸은 해당 조건을 적용하지 않습니다. 대소문자·띄어쓰기는 구분합니다.", 16);
        all = field("모두 포함 (AND) · 예: 카드사용내역", r.optString("all"));
        any = field("하나 이상 포함 (OR) · 선택사항", r.optString("any"));
        exclude = field("하나라도 포함하면 제외 · 예: 1569", r.optString("exclude"));
        label("Step 3 · 전송번호", 22);
        destinations = field("수신번호 1개 이상 · 쉼표/줄바꿈 구분\n숫자 8~15자리, 국제번호는 +로 시작 가능 · 중복은 제거합니다.", r.optString("destinations"));
        label("전달 내용: [자동전송] + 공백 + 받은 본문\n자동발송 켜기/끄기는 메인 화면에서 설정합니다.", 16);
        button("규칙 저장", this::save);
        button("목록으로 (변경 취소)", this::list);
        if (r.has("id")) button("규칙 삭제", () -> new AlertDialog.Builder(this).setMessage("이 규칙을 삭제할까요?")
            .setNegativeButton("취소", null).setPositiveButton("삭제", (d,w) -> {
                JSONArray next = withoutCurrent();
                if (Rules.save(this, next)) list(); else error("저장하지 못했습니다.");
            }).show());
    }
    private JSONObject draft() throws org.json.JSONException {
        return new JSONObject().put("id", editing).put("name", name.getText().toString().trim())
            .put("channel", channel.getSelectedItem().toString()).put("enabled", enabled.isChecked())
            .put("senders", senders.getText().toString()).put("all", all.getText().toString())
            .put("any", any.getText().toString()).put("exclude", exclude.getText().toString())
            .put("destinations", destinations.getText().toString());
    }
    @Override public void onSaveInstanceState(Bundle out) {
        if (editing != null) try { out.putString("draft", draft().toString()); } catch (Exception ignored) { }
        super.onSaveInstanceState(out);
    }
    private JSONArray withoutCurrent() {
        JSONArray next = new JSONArray(), old = Rules.load(this);
        for (int i = 0; i < old.length(); i++) {
            JSONObject r = old.optJSONObject(i);
            if (r != null && !editing.equals(r.optString("id"))) next.put(r);
        }
        return next;
    }
    private void save() {
        try {
            JSONObject r = draft();
            if (r.optString("name").isEmpty()) { name.setError("규칙 이름을 입력해 주세요."); return; }
            if (RuleMatcher.terms(r.optString("senders")).isEmpty()) { senders.setError("발신인을 1개 이상 입력해 주세요."); return; }
            LinkedHashSet<String> numbers = new LinkedHashSet<>();
            for (String raw : RuleMatcher.terms(r.optString("destinations"))) {
                String n = RuleMatcher.number(raw);
                if (!n.matches("\\+?[0-9]{8,15}")) { destinations.setError("전송번호를 확인해 주세요: " + raw); return; }
                numbers.add(n);
            }
            if (numbers.isEmpty()) { destinations.setError("전송번호를 1개 이상 입력해 주세요."); return; }
            r.put("destinations", String.join("\n", numbers));
            JSONArray next = withoutCurrent(); next.put(r);
            if (Rules.save(this, next)) list(); else error("규칙을 저장하지 못했습니다.");
        } catch (org.json.JSONException e) { error("규칙을 저장하지 못했습니다."); }
    }
    private void error(String message) { new AlertDialog.Builder(this).setMessage(message).setPositiveButton("확인", null).show(); }
}



