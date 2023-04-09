package com.feiwin.xmppstompserver.exception;

import lombok.Getter;

@Getter
public class MessageException extends RuntimeException {
    private final String uuid;

    public MessageException(String message) {
        this(message, null);
    }

    public MessageException(String message, String uuid) {
        super(message);
        this.uuid = uuid;
    }
}
