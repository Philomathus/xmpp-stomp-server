package com.feiwin.xmppstompserver.dto;

import com.feiwin.xmppstompserver.annotation.FieldName;
import lombok.Data;

@Data
public class RoomSettings {

    @FieldName("muc#roomconfig_persistentroom")
    private Boolean isPersistent;

    @FieldName("muc#roomconfig_maxusers")
    private Integer maxUsers;

}
