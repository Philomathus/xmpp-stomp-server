package com.feiwin.xmppstompserver.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RoomUser {
    private String uuid;
    private String roomId;
    private String username;
}
