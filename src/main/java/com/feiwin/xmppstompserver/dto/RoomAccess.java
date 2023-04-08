package com.feiwin.xmppstompserver.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RoomAccess {

    private String roomId;

    private String username;

}
