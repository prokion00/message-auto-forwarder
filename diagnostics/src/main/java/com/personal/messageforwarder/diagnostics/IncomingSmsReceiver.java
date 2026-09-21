package com.personal.messageforwarder.diagnostics;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Telephony;
import android.telephony.SmsMessage;

public class IncomingSmsReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (!Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(intent.getAction())) return;
        SmsMessage[] messages = Telephony.Sms.Intents.getMessagesFromIntent(intent);
        if (messages == null || messages.length == 0) return;
        StringBuilder body = new StringBuilder();
        for (SmsMessage message : messages) {
            if (message.getMessageBody() != null) body.append(message.getMessageBody());
        }
        String sender = messages[0].getOriginatingAddress();
        ReceiveLog.record(context, "sms:" + intent.getIntExtra("subscription", -1) + ":" + sender + ":" + messages[0].getTimestampMillis(),
            "SMS", sender, body.toString(), "", "문자 수신 이벤트", System.currentTimeMillis(),false);
    }
}

