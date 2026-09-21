package com.personal.messageforwarder.diagnostics;

import android.app.Activity;
import android.os.Bundle;
import android.widget.*;

public class AutoHistoryActivity extends Activity {
    private TextView content;
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        LinearLayout layout=AppUi.page(this);
        layout.addView(AppUi.title(this,"발송 내역"));
        TextView caption=AppUi.text(this,"최근 100건",15,AppUi.MUTED);layout.addView(caption);AppUi.gap(this,layout,14);
        Button refresh=AppUi.button(this,"새로고침",false);refresh.setOnClickListener(v->refresh());layout.addView(refresh);AppUi.gap(this,layout,14);
        TextView note=AppUi.text(this,"발송 성공은 휴대폰의 발송 결과입니다. 상대방 수신은 별도로 확인해 주세요. 실패·미확인은 자동 재시도하지 않습니다.",14,AppUi.MUTED);layout.addView(note);AppUi.gap(this,layout,18);
        content=AppUi.text(this,"",16,AppUi.INK);content.setTextIsSelectable(true);content.setPadding(AppUi.dp(this,16),AppUi.dp(this,16),AppUi.dp(this,16),AppUi.dp(this,16));content.setBackground(AppUi.panel(android.graphics.Color.WHITE,android.graphics.Color.rgb(220,230,227),AppUi.dp(this,16)));layout.addView(content);
        AppUi.showScrollable(this,layout);
    }
    @Override public void onResume(){super.onResume();refresh();}
    private void refresh(){try(AutoStore store=new AutoStore(this)){content.setText(store.history());}}
}
