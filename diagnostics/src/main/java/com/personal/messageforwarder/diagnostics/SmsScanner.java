package com.personal.messageforwarder.diagnostics;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.provider.Telephony;

final class SmsScanner {
    static void scan(Context c) {
        if (c.checkSelfPermission(Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) return;
        long since = ReceiveLog.prefs(c).getLong("since", System.currentTimeMillis());
        int found = 0;
        try (Cursor rows = c.getContentResolver().query(Telephony.Sms.Inbox.CONTENT_URI,
                new String[]{"_id", "address", "body", "date"}, "date>=?", new String[]{Long.toString(since)}, "date DESC")) {
            if (rows != null) while (rows.moveToNext() && found < 50) {
                found++;
                ReceiveLog.record(c, "sms-store:" + rows.getString(0), "SMS", rows.getString(1), rows.getString(2), "",
                    "SMS 저장소 직접 확인 · 수신 시각 " + android.text.format.DateFormat.format("MM-dd HH:mm:ss", rows.getLong(3))
                    + " (수신 이벤트와 별도 표시, 발송은 중복 방지)",rows.getLong(3),false);
            }
            ReceiveLog.prefs(c).edit().putString("smsScanStatus", "SMS 저장소 조회 성공 / 진단 시작 이후 " + found + "건")
                .putLong("smsScanAt", System.currentTimeMillis()).apply();
        } catch (RuntimeException e) {
            ReceiveLog.prefs(c).edit().putString("smsScanStatus", "SMS 조회 오류: " + e.getClass().getSimpleName()).apply();
        }
    }
}

