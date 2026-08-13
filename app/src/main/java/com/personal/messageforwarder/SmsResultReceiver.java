package com.personal.messageforwarder;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.SmsManager;

public final class SmsResultReceiver extends BroadcastReceiver {
    public static final String ACTION_SENT="com.personal.messageforwarder.SMS_SENT";
    public static final String ACTION_DELIVERED="com.personal.messageforwarder.SMS_DELIVERED";

    @Override public void onReceive(Context c,Intent i) {
        String id=i.getStringExtra("historyId"); if(id==null)return;
        boolean sent=ACTION_SENT.equals(i.getAction()); boolean success=getResultCode()==Activity.RESULT_OK;
        HistoryStore.updateResult(c,id,sent?"SENT":"DELIVERED",success,resultText(sent,getResultCode()));
    }

    private String resultText(boolean sent,int code) {
        if(code==Activity.RESULT_OK)return sent?"통신사 발송 요청 접수 완료":"수신 단말 전달 완료";
        if(!sent)return "수신 단말 전달 실패 (코드 "+code+")";
        switch(code) {
            case SmsManager.RESULT_ERROR_GENERIC_FAILURE:return "일반 발송 오류";
            case SmsManager.RESULT_ERROR_NO_SERVICE:return "통신 서비스 없음";
            case SmsManager.RESULT_ERROR_NULL_PDU:return "잘못된 SMS 데이터";
            case SmsManager.RESULT_ERROR_RADIO_OFF:return "모바일 네트워크 꺼짐";
            case SmsManager.RESULT_ERROR_LIMIT_EXCEEDED:return "SMS 발송 한도 초과";
            case SmsManager.RESULT_ERROR_SHORT_CODE_NOT_ALLOWED:return "단축번호 발송 차단";
            default:return "SMS 발송 실패 (코드 "+code+")";
        }
    }
}
