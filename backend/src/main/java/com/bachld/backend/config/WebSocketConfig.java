package com.bachld.backend.config;

import com.bachld.backend.model.Classes;
import com.bachld.backend.model.Teacher;
import com.bachld.backend.repository.ClassRepository;
import com.bachld.backend.repository.TeacherRepository;
import com.bachld.backend.service.JwtService;
import com.bachld.backend.service.UserPrincipalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.List;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
@Slf4j
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtService jwtService;
    private final UserPrincipalService userDetailsService;
    private final TeacherRepository teacherRepository;
    private final ClassRepository classRepository;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
                
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor =
                        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                if (accessor == null) return message;

                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    String token = extractToken(accessor);
                    if (token != null) {
                        String userId = jwtService.extractId(token);
                        if (userId != null) {
                            UserDetails userDetails = userDetailsService.loadUserByUsername(userId);
                            if (jwtService.validateToken(token, userDetails)) {
                                UsernamePasswordAuthenticationToken authentication =
                                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                                accessor.setUser(authentication);
                            }
                        }
                    }
                } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
                    String destination = accessor.getDestination();
                    if (destination != null && destination.startsWith("/topic/class/")) {
                        if (accessor.getUser() == null) {
                            throw new IllegalArgumentException("Unauthorized subscription");
                        }
                        
                        Integer userId = Integer.valueOf(accessor.getUser().getName());
                        Integer classId = Integer.valueOf(destination.replace("/topic/class/", ""));
                        
                        // Check if teacher managing this class
                        Teacher teacher = teacherRepository.findByUserId(userId).orElse(null);
                        Classes clazz = classRepository.findById(classId).orElse(null);
                        
                        if (teacher == null || clazz == null || !clazz.getTeacherId().equals(teacher.getId())) {
                            throw new IllegalArgumentException("Bạn không được phép truy cập lớp này");
                        }
                    }
                }
                
                return message;
            }

            private String extractToken(StompHeaderAccessor accessor) {
                List<String> authorization = accessor.getNativeHeader("Authorization");
                if (authorization != null && !authorization.isEmpty()) {
                    String bearerToken = authorization.get(0);
                    if (bearerToken.startsWith("Bearer ")) {
                        return bearerToken.substring(7);
                    }
                }
                return null;
            }
        });
    }
}
