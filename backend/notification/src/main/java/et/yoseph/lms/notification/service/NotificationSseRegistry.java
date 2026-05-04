package et.yoseph.lms.notification.service;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class NotificationSseRegistry {

    private final Map<String, CopyOnWriteArrayList<SseEmitter>> emittersByUser = new ConcurrentHashMap<>();

    public SseEmitter subscribe(String userSub) {
        SseEmitter emitter = new SseEmitter(0L);
        if (userSub == null || userSub.isBlank()) {
            emitter.completeWithError(new IllegalArgumentException("userSub is required"));
            return emitter;
        }
        CopyOnWriteArrayList<SseEmitter> list =
                emittersByUser.computeIfAbsent(userSub, k -> new CopyOnWriteArrayList<>());
        list.add(emitter);
        Runnable detach = () -> list.remove(emitter);
        emitter.onCompletion(detach);
        emitter.onTimeout(detach);
        emitter.onError(e -> detach.run());
        return emitter;
    }

    public void sendPayload(String userSub, Map<String, Object> payload) {
        if (userSub == null || userSub.isBlank()) {
            return;
        }
        CopyOnWriteArrayList<SseEmitter> list = emittersByUser.get(userSub);
        if (list == null || list.isEmpty()) {
            return;
        }
        for (SseEmitter emitter : list) {
            try {
                emitter.send(SseEmitter.event().name("message").data(payload));
            } catch (IOException | IllegalStateException ex) {
                emitter.completeWithError(ex);
            }
        }
    }
}
