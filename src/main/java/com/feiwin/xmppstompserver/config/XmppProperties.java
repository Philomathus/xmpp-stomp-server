package com.feiwin.xmppstompserver.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * A connection contains common information needed to connect to an XMPP server
 * and sign in.
 */
@Data
@ConfigurationProperties(prefix = "openfire")
public class XmppProperties {

    /**
     * The address of the server.
     */
    private String host;

    /**
     * The port to use (usually 5222).
     */
    private int port;

    /**
     * The XMPP domain is what follows after the '@' sign in XMPP addresses (JIDs).
     */
    private String domain;

    /**
     * Usually has the form: conference.[domain]
     */
    private String roomDomain;

    /**
     * Admin account credentials.
     * This is used to create accounts that do not exist yet.
     */
    private String adminUsername;
    private String adminPassword;

    /**
     * The default password for all Openfire users except the admin.
     */
    private String userPassword;

}
