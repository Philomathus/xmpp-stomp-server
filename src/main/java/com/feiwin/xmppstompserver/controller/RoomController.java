package com.feiwin.xmppstompserver.controller;

import com.feiwin.xmppstompserver.exception.MessageException;
import com.feiwin.xmppstompserver.model.Response;
import com.feiwin.xmppstompserver.model.RoomMessage;
import com.feiwin.xmppstompserver.model.RoomUser;
import com.feiwin.xmppstompserver.model.RoomCreation;
import com.feiwin.xmppstompserver.service.ImService;
import jakarta.annotation.Resource;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.security.Principal;


@Controller
@MessageMapping("/room")
public class RoomController {
    @Resource
    private ImService imService;

    @MessageMapping("/message")
    @SendToUser("/queue/response")
    public Response sendMessage(RoomMessage roomMessage, Principal principal) {
        roomMessage.setUsername(principal.getName());
        imService.sendRoomMessage(roomMessage);
        return Response.success(roomMessage.getUuid());
    }

    @MessageMapping("/create")
    @SendToUser("/queue/response")
    public Response create(RoomCreation roomCreation, Principal principal) {
        roomCreation.setUsername(principal.getName());
        imService.createRoom(roomCreation);
        return Response.success(roomCreation.getUuid());
    }

    @MessageMapping("/join")
    @SendToUser("/queue/response")
    public Response join(RoomUser roomUser, Principal principal) {
        roomUser.setUsername(principal.getName());
        imService.joinRoom(roomUser);
        return Response.success(roomUser.getUuid());
    }

    @MessageMapping("/leave")
    @SendToUser("/queue/response")
    public Response leave(RoomUser roomUser, Principal principal) {
        roomUser.setUsername(principal.getName());
        imService.leaveRoom(roomUser);
        return Response.success(roomUser.getUuid());
    }

    @MessageMapping("/destroy")
    @SendToUser("/queue/response")
    public Response destroy(RoomUser roomUser, Principal principal) {
        roomUser.setUsername(principal.getName());
        imService.destroyRoom(roomUser);
        return Response.success(roomUser.getUuid());
    }

    @MessageExceptionHandler
    @SendToUser("/queue/error")
    public Response handleException(MessageException exception) {
        return Response.fail(exception.getMessage(), exception.getUuid());
    }
}
