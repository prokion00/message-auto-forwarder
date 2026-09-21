package com.personal.messageforwarder.diagnostics;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Telephony;

final class MmsScanner {
    static void scan(Context c) {
        if (c.checkSelfPermission(Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) return;
        long since = ReceiveLog.prefs(c).getLong("since", System.currentTimeMillis()) / 1000;
        try (Cursor rows = c.getContentResolver().query(Telephony.Mms.CONTENT_URI,
                new String[]{"_id", "m_type", "date"}, "msg_box=1 AND date>=?", new String[]{Long.toString(since)}, "date DESC")) {
            if (rows == null) return;
            int count = 0;
            while (rows.moveToNext() && count++ < 50) {
                String id = rows.getString(0);
                if (rows.getInt(1) != 132) continue; // Only downloaded Retrieve.conf; not a notification placeholder.
                String sender = "확인 불가";
                try (Cursor addr = c.getContentResolver().query(Telephony.Mms.Addr.getAddrUriForMessage(id),
                        new String[]{"address"}, "type=137", null, null)) {
                    if (addr != null && addr.moveToFirst()) sender = addr.getString(0);
                }
                StringBuilder text = new StringBuilder();
                int attachments = 0;
                boolean unavailable = false;
                try (Cursor parts = c.getContentResolver().query(Telephony.Mms.Part.getPartUriForMessage(id),
                        new String[]{"ct", "text"}, null, null, "seq ASC")) {
                    if (parts == null) continue;
                    while (parts.moveToNext()) {
                        String type = parts.getString(0);
                        if ("text/plain".equals(type)) {
                            String part = parts.getString(1);
                            if (part == null) unavailable = true;
                            else text.append(part);
                        } else if (!"application/smil".equals(type)) attachments++;
                    }
                }
                ReceiveLog.record(c, "mms:" + id, "MMS", sender, text.toString(), "",
                    "MMS 저장소 확인 · 첨부 " + attachments + "개 (첨부 내용 미열람)"
                    + (unavailable ? "\n일부 텍스트를 읽을 수 없음" : ""),rows.getLong(2)*1000,unavailable);
            }
        } catch (RuntimeException e) {
            ReceiveLog.add(c, "mms-error", "MMS 진단", "", "", "조회 오류: " + e.getClass().getSimpleName());
        }
    }
}

