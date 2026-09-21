package com.personal.messageforwarder.diagnostics;
public class RuleMatcherTest {
    private static int count;
    private static void check(boolean result) { if (!result) throw new AssertionError("case " + (count + 1)); count++; }
    private static String match(String body) {
        return RuleMatcher.evaluate("SMS", "010-1234-5678", "카드사용내역", "", "1569", "SMS", "+821012345678", body, false);
    }
    public static void main(String[] args) {
        check(match("카드사용내역 1000원").equals("일치"));
        check(!match("카드사용내역 1569 1000원").equals("일치"));
        check(!match("광고").equals("일치"));
        check(!match("").equals("일치"));
        check(RuleMatcher.evaluate("카카오톡", "홍길동", "도착", "서울,부산", "취소", "카카오톡", "홍길동", "서울 도착", false).equals("일치"));
        check(!RuleMatcher.evaluate("카카오톡", "홍길동", "도착", "서울,부산", "취소", "카카오톡", "홍길동", "대전 도착", false).equals("일치"));
        check(!RuleMatcher.evaluate("전체", "홍길동", "", "", "", "카카오톡", "홍길동", "도착", true).equals("일치"));
        check(!RuleMatcher.evaluate("전체", "홍길동", "", "", "", "RCS", "홍길동", "도착", false).equals("일치"));
        check(!RuleMatcher.evaluate("SMS", "01011112222", "", "", "", "SMS", "010111122223", "도착", false).equals("일치"));
        check(RuleMatcher.terms("A,A\nB\r\n").size() == 2);
        check(RuleMatcher.evaluate("메시지 알림", "♥EUN♥", "1*8*", "", "제외", "메시지 알림", "\u2068♥EUN♥\u2069", "카드 1*8*", false).equals("일치"));
        check(RuleMatcher.evaluate("전체", "01030174370", "", "", "", "메시지 알림", "+82 10-3017-4370", "테스트", false).equals("일치"));
        check(!RuleMatcher.evaluate("전체", "01030174370", "", "", "", "메시지 알림", "♥EUN♥", "테스트", false).equals("일치"));
        check(!RuleMatcher.evaluate("SMS", "♥EUN♥", "", "", "", "메시지 알림", "♥EUN♥", "테스트", false).equals("일치"));
        check(!RuleMatcher.evaluate("메시지 알림", "♥EUN♥", "", "", "", "메시지 알림", "♥EUN♥", "테스트", true).equals("일치"));
        System.out.println("PASS: " + count + " rule matching cases");
    }
}

