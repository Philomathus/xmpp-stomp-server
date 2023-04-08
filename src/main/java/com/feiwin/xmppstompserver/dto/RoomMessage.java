package com.feiwin.xmppstompserver.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RoomMessage {
    private String content;
    private String roomId;
    private String from;
}
