package com.feiwin.xmppstompserver.config;

import com.feiwin.xmppstompserver.constant.Constants;
import com.feiwin.xmppstompserver.service.TokenService;
import com.feiwin.xmppstompserver.service.XmppService;
import com.sun.security.auth.UserPrincipal;
import io.jsonwebtoken.Claims;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.util.CollectionUtils;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import static com.feiwin.xmppstompserver.constant.Constants.CONNECTIONS;
import static org.springframework.messaging.simp.stomp.StompCommand.*;
import java.util.List;
import java.util.Objects;

@Configuration
@EnableWebSocketMessageBroker
@Order(Ordered.HIGHEST_PRECEDENCE + 99)
@Slf4j
public class WebSocketRegistrationConfig implements WebSocketMessageBrokerConfigurer {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private TokenService tokenService;

    @Resource
    private XmppService xmppService;

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor( message, StompHeaderAccessor.class );

                assert accessor != null;

                if (Objects.requireNonNull(accessor.getCommand()) == CONNECT) {
                    List<String> authorization = accessor.getNativeHeader("Token");

                    if (CollectionUtils.isEmpty(authorization)) {
                        throw new RuntimeException("No token was passed!");
                    }

                    String token = authorization.get(0);
                    Claims claims = tokenService.parseToken(token);
                    String memberId = (String) claims.get(Constants.MEMBER_ID);
                    String username = (String) claims.get(Constants.USERNAME);

                    if(StringUtils.isBlank(memberId)) {
                        throw new RuntimeException("The memberId is missing!");
                    }

                    if(StringUtils.isBlank(username)) {
                        throw new RuntimeException("The username is missing!");
                    }

                    String redisToken = stringRedisTemplate.opsForValue().get(Constants.USER_JJWT_KEY + memberId);

                    if(!StringUtils.equals( redisToken, token )) {
                        throw new RuntimeException("The token is invalid!");
                    }

                    register(username);

                    accessor.setUser(new UserPrincipal(username));
                }

                return message;
            }
        });
    }

    private void register(String username) {
        log.info("Starting XMPP session '{}'.", username);

        XMPPTCPConnection connection = xmppService.connect(username);

        if (connection == null) {
            log.info("XMPP connection was not established. Closing websocket session...");
            throw new RuntimeException("Something went wrong establishing a connection");
        }

        try {
            try {
                log.info("Login into account.");
                xmppService.login(connection);
            } catch (Exception ex) {
                log.info("The user does not exist. Creating a new account.");
                xmppService.createAccount(username);
                xmppService.login(connection);
            }
        } catch(Exception e) {
            throw new RuntimeException("There was a problem logging in");
        }

        CONNECTIONS.put(username, connection);

        log.info("Connection was stored");

        xmppService.addIncomingMessageListener(connection);
    }

}