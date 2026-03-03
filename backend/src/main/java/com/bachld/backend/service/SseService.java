package com.bachld.backend.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.Map;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class SseService {

    Map<Integer, SseEmitter> emitters = new HashMap<>();

    public SseEmitter subscribe(int userId) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

        emitter.onCompletion(() -> emitters.remove(userId));
        emitter.onTimeout(() -> emitters.remove(userId));
        emitter.onError((e) -> emitters.remove(userId));

        emitters.put(userId, emitter);

        return emitter;
    }

    @EventListener
    public void handleNotificationEvent(int userId) {
        if (emitters.containsKey(userId)) {
            SseEmitter emitter = emitters.get(userId);

            try {
                emitter.send(SseEmitter.event()
                        .name("new-notification")
                        .data(Map.of("newNotification", true)));
            }
            catch (Exception e) {
                emitters.remove(userId);
            }
        }
    }

    @Async
    public void handleEvent() throws InterruptedException {
        Thread.sleep(1000);
        handleNotificationEvent(1);
    }

}