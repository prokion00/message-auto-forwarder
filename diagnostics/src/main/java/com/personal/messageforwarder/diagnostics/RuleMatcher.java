package com.personal.messageforwarder.diagnostics;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

final class RuleMatcher {
    static List<String> terms(String value) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        for (String item : value.split("[,\\r\\n]+")) if (!item.trim().isEmpty()) out.add(item.trim());
        return new ArrayList<>(out);
    }
    static String name(String value) { return value.replaceAll("[\u200E\u200F\u202A-\u202E\u2066-\u2069]", "").trim(); }
    static String number(String value) {
        String n = value.replaceAll("[\\s()-]", "");
        if (n.startsWith("+82")) n = "0" + n.substring(3);
        return n;
    }
    static String evaluate(String channel, String senders, String all, String any, String excluded,
            String source, String sender, String body, boolean uncertain) {
        if (!channel.equals("전체") && !channel.equals(source)) return "수신 종류 불일치";
        if (!source.equals("SMS") && !source.equals("MMS") && !source.equals("카카오톡") && !source.equals("메시지 알림")) return "지원하지 않는 수신 종류";
        if (uncertain || sender.isEmpty() || body.isEmpty()) return "발신인 또는 본문 확인 필요";
        boolean matched = false;
        for (String item : terms(senders)) {
            if (source.equals("카카오톡") ? name(item).equals(name(sender)) : source.equals("메시지 알림") ? (name(item).equals(name(sender)) || (number(item).matches("\\+?[0-9]+") && number(item).equals(number(sender)))) : number(item).equals(number(sender))) matched = true;
        }
        if (!matched) return "발신인 불일치";
        for (String word : terms(excluded)) if (body.contains(word)) return "제외 단어 포함: " + word;
        for (String word : terms(all)) if (!body.contains(word)) return "필수 단어 없음: " + word;
        List<String> choices = terms(any);
        if (!choices.isEmpty()) {
            boolean found = false;
            for (String word : choices) if (body.contains(word)) found = true;
            if (!found) return "선택 단어가 모두 없음";
        }
        return "일치";
    }
}

