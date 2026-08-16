package com.smartdoor.service;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class SseService {
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(0L);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(error -> emitters.remove(emitter));
        try { emitter.send(SseEmitter.event().name("connected").data("ok")); }
        catch (IOException ignored) { emitters.remove(emitter); }
        return emitter;
    }

    public void broadcast(String eventName, Object value) {
        emitters.forEach(emitter -> {
            try { emitter.send(SseEmitter.event().name(eventName).data(value)); }
            catch (IOException exception) { emitters.remove(emitter); }
        });
    }
}

