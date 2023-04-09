package com.feiwin.xmppstompserver.service;

import com.feiwin.xmppstompserver.annotation.FieldName;
import com.feiwin.xmppstompserver.exception.MessageException;
import com.feiwin.xmppstompserver.model.PrivateMessage;
import com.feiwin.xmppstompserver.model.RoomUser;
import com.feiwin.xmppstompserver.model.RoomCreation;
import com.feiwin.xmppstompserver.model.RoomMessage;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static com.feiwin.xmppstompserver.constant.Constants.CONNECTIONS;

@Service
@Slf4j
public class ImService {

    @Resource
    private XmppService xmppService;
    
    public void sendPrivateMessage(PrivateMessage privateMessage) {
        try {
            xmppService.sendPrivateMessage(CONNECTIONS.get(privateMessage.getUsername()), privateMessage.getContent(), privateMessage.getTo());
        } catch(Exception e) {
            throw new MessageException(e.getMessage(), privateMessage.getUuid());
        }
    }
    
    public void sendRoomMessage(RoomMessage roomMessage) {
        try {
            xmppService.sendRoomMessage(CONNECTIONS.get(roomMessage.getUsername()), roomMessage.getRoomId(), roomMessage.getContent());
        } catch(Exception e) {
            throw new MessageException(e.getMessage(), roomMessage.getUuid());
        }
    }

    public void createRoom(RoomCreation roomCreation) {
        Map<String, String> settings = new HashMap<>();

        try {
            for(Field field : RoomCreation.class.getDeclaredFields()) {
                FieldName fieldNameAnnotation = field.getAnnotation(FieldName.class);

                if(fieldNameAnnotation != null) {
                    field.setAccessible(true);
                    settings.put(fieldNameAnnotation.value(), Objects.toString(field.get(roomCreation), null));
                }
            }

            xmppService.createRoom(CONNECTIONS.get(roomCreation.getUsername()), roomCreation.getRoomId(), roomCreation.getUsername(), settings);
        } catch(Exception e) {
            throw new MessageException(e.getMessage(), roomCreation.getUuid());
        }
    }
    
    public void joinRoom(RoomUser roomUser) {
        try {
            xmppService.joinRoom(CONNECTIONS.get(roomUser.getUsername()), roomUser.getRoomId(), roomUser.getUsername());
        } catch(Exception e) {
            throw new MessageException(e.getMessage(), roomUser.getUuid());
        }
    } 
    
    public void leaveRoom(RoomUser roomUser) {
        try {
            xmppService.leaveRoom(CONNECTIONS.get(roomUser.getUsername()), roomUser.getRoomId());
        } catch(Exception e) {
            throw new MessageException(e.getMessage(), roomUser.getUuid());
        }
    }
    
    public void destroyRoom(RoomUser roomUser) {
        try {
            xmppService.destroyRoom(CONNECTIONS.get(roomUser.getUsername()), roomUser.getRoomId());
        } catch(Exception e) {
            throw new MessageException(e.getMessage(), roomUser.getUuid());
        }
    }

}
