package com.feiwin.xmppstompserver.model;

import lombok.Data;

@Data
public class TokenAuth {
    private String memberId;
    private String username;
}