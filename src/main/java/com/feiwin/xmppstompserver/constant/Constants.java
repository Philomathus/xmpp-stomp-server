package com.feiwin.xmppstompserver.constant;

import jakarta.websocket.Session;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;

import java.util.HashMap;
import java.util.Map;

public class Constants {

    /**
     * Principal.name -> XMPPTCPConnection
     */
    public static final Map<String, XMPPTCPConnection> CONNECTIONS = new HashMap<>();

    public final static String MEMBER_ID = "memberId";

    public final static String USERNAME = "username";

    public static final String USER_JJWT_KEY = "im:user:jjwt:";

}