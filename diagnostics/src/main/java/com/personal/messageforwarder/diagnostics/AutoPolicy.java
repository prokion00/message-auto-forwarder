package com.personal.messageforwarder.diagnostics;

final class AutoPolicy {
    static String blocked(String body, long eventAt, long enabledAt) {
        if (eventAt <= 0 || eventAt <= enabledAt) return "자동발송 시작 이전 메시지";
        if (body == null || body.trim().isEmpty()) return "본문 없음";
        if (body.stripLeading().startsWith("[자동전송]")) return "자동전송 메시지 재전달 방지";
        if (body.contains("민감한 알림 내용") || body.contains("Sensitive notification content hidden")) return "알림 내용 확인 불가";
        return "";
    }
}
