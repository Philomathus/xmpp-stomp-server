package com.feiwin.xmppstompserver.controller;

import com.feiwin.xmppstompserver.exception.MessageException;
import com.feiwin.xmppstompserver.model.PrivateMessage;
import com.feiwin.xmppstompserver.model.Response;
import com.feiwin.xmppstompserver.service.ImService;
import com.feiwin.xmppstompserver.service.PublishingService;
import com.feiwin.xmppstompserver.service.XmppService;
import jakarta.annotation.Resource;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.security.Principal;

import static com.feiwin.xmppstompserver.constant.Constants.CONNECTIONS;
import static com.feiwin.xmppstompserver.constant.Status.*;

@Controller
@MessageMapping("/private-chat")
public class PrivateChatController {

    @Resource
    private ImService imService;

    @MessageMapping("/message")
    @SendToUser("/queue/response")
    public Response sendMessage(PrivateMessage privateMessage, Principal principal) {
        privateMessage.setUsername(principal.getName());
        imService.sendPrivateMessage(privateMessage);
        return Response.success(privateMessage.getUuid());
    }

    @MessageExceptionHandler
    @SendToUser("/queue/error")
    public Response handleException(MessageException exception) {
        return Response.fail(exception.getMessage(), exception.getUuid());
    }

}
