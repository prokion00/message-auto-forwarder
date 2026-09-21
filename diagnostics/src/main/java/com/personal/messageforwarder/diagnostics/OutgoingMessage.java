package com.personal.messageforwarder.diagnostics;

final class OutgoingMessage {
    static String format(String originalBody) {
        return "[자동전송] " + originalBody;
    }
}
