package com.personal.messageforwarder.diagnostics;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.telephony.SmsManager;

public class SendResultReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (!(context.getPackageName() + ".SENT").equals(intent.getAction())) return;
        SharedPreferences prefs = context.getSharedPreferences("test", Context.MODE_PRIVATE);
        String id = intent.getStringExtra("id");
        if (id == null || !id.equals(prefs.getString("id", ""))) return;
        int code = getResultCode();
        String result;
        switch (code) {
            case Activity.RESULT_OK:
                result = "발송 성공 — 상대 휴대폰에서 실제 수신 여부를 확인해 주세요.";
                break;
            case SmsManager.RESULT_ERROR_RADIO_OFF: result = "발송 실패: 무선 통신이 꺼져 있습니다."; break;
            case SmsManager.RESULT_ERROR_NO_SERVICE: result = "발송 실패: 통신 서비스에 연결되지 않았습니다."; break;
            case SmsManager.RESULT_ERROR_LIMIT_EXCEEDED: result = "발송 실패: 문자 발송 제한에 도달했습니다."; break;
            default: result = "발송 실패: 결과 코드 " + code + " / 모뎀 코드 " + intent.getIntExtra("errorCode", 0);
        }
        prefs.edit().putBoolean("pending", false).putString("result", result).commit();
    }
}
