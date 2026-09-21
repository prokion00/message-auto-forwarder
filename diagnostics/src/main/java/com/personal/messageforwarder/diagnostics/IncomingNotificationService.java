package com.personal.messageforwarder.diagnostics;

import android.app.Notification;
import android.app.Person;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import java.util.List;

public class IncomingNotificationService extends NotificationListenerService {
    @Override public void onNotificationPosted(StatusBarNotification event) {
        String pkg = event.getPackageName();
        boolean kakao = "com.kakao.talk".equals(pkg);
        if (!kakao && !"com.samsung.android.messaging".equals(pkg) && !"com.google.android.apps.messaging".equals(pkg)) return;
        Notification n = event.getNotification();
        if ((n.flags & Notification.FLAG_GROUP_SUMMARY) != 0) return;
        Bundle e = n.extras;
        if (e == null) return;
        String source = kakao ? "카카오톡 알림" : "메시지 알림";
        String title = str(e.getCharSequence(Notification.EXTRA_TITLE));
        String note = "앱: " + pkg + " / 알림 제목·대화방: " + title + "\n알림에 제공된 정보만 표시 · 숨김/잘림 가능";
        List<Notification.MessagingStyle.Message> messages = Notification.MessagingStyle.Message.getMessagesFromBundleArray(e.getParcelableArray(Notification.EXTRA_MESSAGES));
        long since = ReceiveLog.prefs(this).getLong("since", Long.MAX_VALUE);
        if (!messages.isEmpty()) {
            for (Notification.MessagingStyle.Message m : messages) {
                if (m.getTimestamp() > 0 && m.getTimestamp() < since) continue;
                Person person = m.getSenderPerson();
                String sender = person == null ? "발신인 미확정 (알림 제목: " + title + ")" : str(person.getName());
                String address = "";
                if (!kakao && person != null && person.getUri() != null && person.getUri().startsWith("tel:")) {
                    String candidate = android.net.Uri.parse(person.getUri()).getSchemeSpecificPart();
                    if (candidate != null && candidate.matches("[+0-9 ()-]+")) address = candidate;
                }
                ReceiveLog.record(this, "notification:" + event.getKey() + ":" + m.getTimestamp() + ":" + sender,
                    source, sender, str(m.getText()), address, note + (kakao ? "" : "\nRCS 포함 메시지 알림 · SMS/MMS와 구분은 미확정\n" + (address.isEmpty() ? "전화번호 미제공: 규칙에 표시된 발신인명을 등록하세요." : "알림이 제공한 발신번호로도 판별 가능")) + (m.getDataMimeType() == null ? "" : "\n첨부 유형: " + m.getDataMimeType()), m.getTimestamp(),person==null || m.getDataMimeType()!=null);
            }
        } else {
            String body = str(e.getCharSequence(Notification.EXTRA_BIG_TEXT));
            if (body.isEmpty()) body = str(e.getCharSequence(Notification.EXTRA_TEXT));
            ReceiveLog.record(this, "notification:" + event.getKey(), source,
                "발신인 후보: " + title, body, "", note + (kakao ? "" : "\nRCS 포함 메시지 알림 · SMS/MMS와 구분은 미확정") + "\n구조화된 발신인 없음: 제목은 대화방 이름일 수도 있습니다.",n.when,true);
        }
    }
    private static String str(CharSequence text) { return text == null ? "" : text.toString(); }
}



