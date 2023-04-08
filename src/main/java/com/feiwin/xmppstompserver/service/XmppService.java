package com.feiwin.xmppstompserver.service;

import com.feiwin.xmppstompserver.annotation.FieldName;
import com.feiwin.xmppstompserver.dto.*;
import com.feiwin.xmppstompserver.config.XmppProperties;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import java.lang.reflect.Field;
import java.util.Objects;

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

    public void joinRoom(XMPPTCPConnection connection, RoomAccess roomAccess) {
        try {
            EntityBareJid jid = JidCreate.entityBareFrom(roomAccess.getRoomId() + "@" + xmppProperties.getRoomDomain());
            MultiUserChatManager multiUserChatManager = MultiUserChatManager.getInstanceFor(connection);
            multiUserChatManager.getRoomInfo(jid);
            MultiUserChat multiUserChat = multiUserChatManager.getMultiUserChat(jid);
            multiUserChat.join(connection.getUser().getResourcepart());

            multiUserChat.addMessageListener( message -> publishingService.sendRoomMessage(message, roomAccess.getUsername()) );
        } catch (Exception e) {
            throw new RuntimeException("Something went wrong joining the room: " + roomAccess.getRoomId());
        }
    }

    public void createRoom(XMPPTCPConnection connection, RoomCreation roomCreation) {

        try {
            MultiUserChat multiUserChat = MultiUserChatManager.getInstanceFor(connection)
                    .getMultiUserChat(JidCreate.entityBareFrom(roomCreation.getRoomId() + "@" + xmppProperties.getRoomDomain()));
            multiUserChat.create(connection.getUser().getResourcepart());

            FillableForm form = multiUserChat.getConfigurationForm().getFillableForm();

            for(Field field : RoomSettings.class.getDeclaredFields()) {
                FieldName fieldNameAnnotation = field.getAnnotation(FieldName.class);

                if(fieldNameAnnotation != null) {
                    field.setAccessible(true);
                    form.setAnswer(fieldNameAnnotation.value(), Objects.toString(field.get(roomCreation.getRoomSettings()), null));
                }
            }

            multiUserChat.sendConfigurationForm(form);
            multiUserChat.addMessageListener( message -> publishingService.sendRoomMessage(message, roomCreation.getOwner()) );
        } catch (Exception e) {
            throw new RuntimeException("Something went wrong creating the room: " + roomCreation.getRoomId());
        }
    }

    public void sendMessage(XMPPTCPConnection connection, PrivateMessage privateMessage) {
        ChatManager chatManager = ChatManager.getInstanceFor(connection);
        try {
            Chat chat = chatManager.chatWith(JidCreate.entityBareFrom(privateMessage.getTo() + "@" + xmppProperties.getDomain()));
            chat.send(privateMessage.getContent());
            log.info("Message sent to user '{}' from user '{}'.", privateMessage.getTo(), connection.getUser());
        } catch (Exception e) {
            throw new RuntimeException("Something went wrong sending a message to: " + privateMessage.getTo());
        }
    }

    public void sendRoomMessage(XMPPTCPConnection connection, RoomMessage roomMessage) {
        try {
            EntityBareJid groupJid = JidCreate.entityBareFrom(roomMessage.getRoomId() + "@" + xmppProperties.getRoomDomain());
            MultiUserChat multiUserChat = MultiUserChatManager.getInstanceFor(connection)
                    .getMultiUserChat(groupJid);
            multiUserChat.sendMessage(connection.getUser().getLocalpart() + ":" + roomMessage.getFrom());
        } catch (Exception e) {
            throw new RuntimeException("Something went wrong sending a message to the room: " + roomMessage.getRoomId());
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
}
