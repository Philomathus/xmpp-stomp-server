package com.feiwin.xmppstompserver.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PrivateMessage {

    private String content;
    private String from;
    private String to;

}
