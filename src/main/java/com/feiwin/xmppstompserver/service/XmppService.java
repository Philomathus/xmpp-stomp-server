package com.feiwin.xmppstompserver.service;

import com.feiwin.xmppstompserver.config.XmppProperties;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.chat2.Chat;
import org.jivesoftware.smack.chat2.ChatManager;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.PresenceBuilder;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smackx.iqregister.AccountManager;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.MultiUserChatManager;
import org.jivesoftware.smackx.xdata.form.FillableForm;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.jid.parts.Localpart;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@EnableConfigurationProperties(XmppProperties.class)
public class XmppService {

    private XMPPTCPConnection adminXmppConnection;

    @Resource
    private XmppProperties xmppProperties;

    @Resource
    private PublishingService publishingService;

    @PostConstruct
    private void loginAdminXmppAccount() {
        adminXmppConnection = connect(xmppProperties.getAdminUsername(), xmppProperties.getAdminPassword());
        login(adminXmppConnection);
    }

    public XMPPTCPConnection connect(String username) {
        return connect(username, xmppProperties.getUserPassword());
    }

    public XMPPTCPConnection connect(String username, String password) {
        XMPPTCPConnection connection;
        try {
            EntityBareJid entityBareJid = JidCreate.entityBareFrom(username + "@" + xmppProperties.getDomain());
            XMPPTCPConnectionConfiguration config = XMPPTCPConnectionConfiguration.builder()
                    .setHost(xmppProperties.getHost())
                    .setPort(xmppProperties.getPort())
                    .setXmppDomain(xmppProperties.getDomain())
                    .setUsernameAndPassword(entityBareJid.getLocalpart(), password)
                    .setSecurityMode(ConnectionConfiguration.SecurityMode.disabled)
                    .setResource(entityBareJid.getResourceOrEmpty())
                    .setSendPresence(true)
                    .build();

            connection = new XMPPTCPConnection(config);
            connection.connect();
        } catch (Exception e) {
            log.info("Could not connect to XMPP server.", e);
            return null;
        }
        return connection;
    }

    public void login(XMPPTCPConnection connection) {
        try {
            connection.login();
        } catch (Exception e) {
            log.error("Login to XMPP server with user {} failed.", connection.getUser(), e);

            throw new RuntimeException("Something went wrong logging in");
        }
        log.info("User '{}' logged in.", connection.getUser());
    }

    public void createAccount(String username) {
        createAccount(username, xmppProperties.getUserPassword());
    }

    public void createAccount(String username, String password) {
        AccountManager accountManager = AccountManager.getInstance(adminXmppConnection);
        accountManager.sensitiveOperationOverInsecureConnection(true);
        try {
            accountManager.createAccount(Localpart.from(username), password);
        } catch (Exception e) {
            throw new RuntimeException("Something went wrong creating the account with username: " + username);
        }

        log.info("Account for user '{}' created.", username);
    }

    public void joinRoom(XMPPTCPConnection connection, String roomId, String username) {
        try {
            EntityBareJid jid = JidCreate.entityBareFrom(roomId + "@" + xmppProperties.getRoomDomain());
            MultiUserChatManager multiUserChatManager = MultiUserChatManager.getInstanceFor(connection);
            multiUserChatManager.getRoomInfo(jid);
            MultiUserChat multiUserChat = multiUserChatManager.getMultiUserChat(jid);
            multiUserChat.join(connection.getUser().getResourcepart());

            multiUserChat.addMessageListener( message -> publishingService.sendRoomMessage(message, username) );
        } catch (Exception e) {
            throw new RuntimeException("Something went wrong joining the room: " + roomId);
        }
    }

    public void createRoom(XMPPTCPConnection connection, String roomId, String owner, Map<String, String> settings) {

        try {
            MultiUserChat multiUserChat = MultiUserChatManager.getInstanceFor(connection)
                    .getMultiUserChat(JidCreate.entityBareFrom(roomId + "@" + xmppProperties.getRoomDomain()));
            multiUserChat.create(connection.getUser().getResourcepart());

            FillableForm form = multiUserChat.getConfigurationForm().getFillableForm();
            settings.forEach(form::setAnswer);
            multiUserChat.sendConfigurationForm(form);
            multiUserChat.addMessageListener( message -> publishingService.sendRoomMessage(message, owner) );
        } catch (Exception e) {
            throw new RuntimeException("Something went wrong creating the room: " + roomId);
        }
    }

    public void sendPrivateMessage(XMPPTCPConnection connection, String content, String to) {
        ChatManager chatManager = ChatManager.getInstanceFor(connection);
        try {
            Chat chat = chatManager.chatWith(JidCreate.entityBareFrom(to + "@" + xmppProperties.getDomain()));
            chat.send(content);
            log.info("Message sent to user '{}' from user '{}'.", to, connection.getUser());
        } catch (Exception e) {
            throw new RuntimeException("Something went wrong sending a message to: " + to);
        }
    }

    public void sendRoomMessage(XMPPTCPConnection connection, String roomId, String content) {
        try {
            EntityBareJid groupJid = JidCreate.entityBareFrom(roomId + "@" + xmppProperties.getRoomDomain());
            MultiUserChat multiUserChat = MultiUserChatManager.getInstanceFor(connection)
                    .getMultiUserChat(groupJid);
            multiUserChat.sendMessage(connection.getUser().getLocalpart() + ":" + content);
        } catch (Exception e) {
            throw new RuntimeException("Something went wrong sending a message to the room: " + roomId);
        }
    }

    public void leaveRoom(XMPPTCPConnection connection, String roomId) {
        try {
            MultiUserChat multiUserChat = MultiUserChatManager.getInstanceFor(connection)
                    .getMultiUserChat(JidCreate.entityBareFrom(roomId + "@" + xmppProperties.getRoomDomain()));
            multiUserChat.leave();
        } catch (Exception e) {
            throw new RuntimeException("Something went wrong leaving the room: " + roomId);
        }
    }

    public void destroyRoom(XMPPTCPConnection connection, String roomId) {
        try {
            MultiUserChat multiUserChat = MultiUserChatManager.getInstanceFor(connection)
                    .getMultiUserChat(JidCreate.entityBareFrom(roomId + '@' + xmppProperties.getRoomDomain()));
            multiUserChat.destroy(null, null);
        } catch(Exception e) {
            throw new RuntimeException("Something went wrong destroying the room: " + roomId);
        }
    }

    public void disconnect(XMPPTCPConnection connection) {
        Presence presence = PresenceBuilder.buildPresence()
                .ofType(Presence.Type.unavailable)
                .build();
        try {
            connection.sendStanza(presence);
            connection.disconnect();
        } catch (Exception e) {
            log.error("XMPP error.", e);
        }
        connection.disconnect();
        log.info("Connection closed for user '{}'.", connection.getUser());
    }

    public void sendStanza(XMPPTCPConnection connection, Presence.Type type) {
        Presence presence = PresenceBuilder.buildPresence()
                .ofType(type)
                .build();
        try {
            connection.sendStanza(presence);
            log.info("Status {} sent for user '{}'.", type, connection.getUser());
        } catch (Exception e) {
            log.error("XMPP error.", e);
            throw new RuntimeException(connection.getUser().toString(), e);
        }
    }

    public void addIncomingMessageListener(XMPPTCPConnection connection) {
        ChatManager chatManager = ChatManager.getInstanceFor(connection);
        chatManager.addIncomingListener((from, message, chat) -> publishingService.sendPrivateMessage(message));
        log.info("Incoming message listener for user '{}' added.", connection.getUser());
    }
}
