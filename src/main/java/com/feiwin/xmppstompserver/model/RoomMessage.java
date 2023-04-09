package com.feiwin.xmppstompserver.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RoomMessage {
    private String uuid;
    private String content;
    private String roomId;
    private String username;
}
