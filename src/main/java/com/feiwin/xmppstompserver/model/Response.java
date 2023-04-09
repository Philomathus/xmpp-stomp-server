package com.feiwin.xmppstompserver.model;

import com.feiwin.xmppstompserver.constant.Status;
import lombok.Builder;
import lombok.Data;
import static com.feiwin.xmppstompserver.constant.Status.*;

@Data
@Builder
public class Response {

    private String uuid;

    private Status status;

    private String message;

    private Object data;

    public static Response success(String uuid) {
        return Response.builder().status(SUCCESS).uuid(uuid).build();
    }

    public static Response fail(String message, String uuid) {
        return Response.builder().status(FAIL).message(message).uuid(uuid).build();
    }
}
