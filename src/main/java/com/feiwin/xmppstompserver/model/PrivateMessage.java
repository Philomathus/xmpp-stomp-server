package com.feiwin.xmppstompserver.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PrivateMessage {
    private String uuid;
    private String content;
    private String username;
    private String to;
}
