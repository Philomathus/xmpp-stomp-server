package com.feiwin.xmppstompserver.service;

import com.feiwin.xmppstompserver.dto.PrivateMessage;
import com.feiwin.xmppstompserver.dto.RoomMessage;
import jakarta.annotation.Resource;
import org.jivesoftware.smack.packet.Message;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class PublishingService {

    @Resource
    private SimpMessagingTemplate messagingTemplate;

    public void sendGlobal(Object message, String destination) {
        messagingTemplate.convertAndSend(destination, message);
    }

    public void sendPrivate(Object message, String destination, String username) {
        messagingTemplate.convertAndSendToUser(username, destination, message);
    }

    public void sendRoomMessage(Message message, String to) {
        String roomId = message.getFrom().getLocalpartOrNull().toString();
        String[] fromAndBodySplit = splitFromAndBody(message.getBody());
        String from = fromAndBodySplit[0];
        String content = fromAndBodySplit[1];

        sendPrivate(
                RoomMessage.builder()
                        .content(content)
                        .roomId(roomId)
                        .from(from)
                        .build()
                ,
                "/topic/room/message",
                to
        );
    }

    private static String[] splitFromAndBody(String fromAndBody) {
        if(fromAndBody == null) {
            return new String[2];
        }

        int indexOfDelimiter = fromAndBody.indexOf(':');
        return new String[] {
                fromAndBody.substring(0, indexOfDelimiter),
                fromAndBody.substring(indexOfDelimiter + 1)
        };
    }

    public void sendPrivateMessage(Message message) {
        String from = message.getFrom().getLocalpartOrNull().toString();
        String to = message.getTo().getLocalpartOrNull().toString();
        String content = message.getBody();

        sendPrivate(
                PrivateMessage.builder()
                        .content(content)
                        .from(from)
                        .to(to)
                        .build()
                ,
                "/topic/private-chat/message",
                to
        );
    }

}
