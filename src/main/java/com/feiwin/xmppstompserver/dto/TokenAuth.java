package com.feiwin.xmppstompserver.dto;

import lombok.Data;

@Data
public class TokenAuth {
    private String memberId;

    private String username;
}