package com.personal.messageforwarder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Telephony;
import android.telephony.SmsMessage;

public final class SmsReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c,Intent i){
        if(!Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(i.getAction()))return;
        SmsMessage[] messages=Telephony.Sms.Intents.getMessagesFromIntent(i);if(messages==null||messages.length==0)return;
        String sender=messages[0].getOriginatingAddress();StringBuilder body=new StringBuilder();for(SmsMessage m:messages)body.append(m.getMessageBody());
        Forwarder.process(c,"SMS",sender==null?"":sender,body.toString());
    }
}
