package com.feiwin.xmppstompserver.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RoomCreation {

    private String roomId;

    private String owner;

    private RoomSettings roomSettings = new RoomSettings();

}
