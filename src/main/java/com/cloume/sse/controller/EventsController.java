package com.cloume.sse.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@RestController
@RequestMapping("/events")
public class EventsController {
    private final ConcurrentHashMap<String, Consumer<String>> listeners = new ConcurrentHashMap<>();

    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamEvents() {
        return Flux.create(emitter -> {
            Consumer<String> listener = emitter::next;
            String id = registerListener(listener);
            emitter.onDispose(() -> unregisterListener(id));
        }, FluxSink.OverflowStrategy.BUFFER);
    }

    private String registerListener(Consumer<String> listener) {
        String id = generateUniqueId();
        listeners.put(id, listener);
        return id;
    }

    private void unregisterListener(String id) {
        listeners.remove(id);
    }

    private String generateUniqueId() {
        // 实现生成唯一ID的逻辑
        return String.valueOf(System.currentTimeMillis());
    }

    // 在类中添加一个ObjectMapper实例
    private final ObjectMapper objectMapper = new ObjectMapper();

    private void sendMessageToAllListeners(String target, String event) {
        Map<String, String> messageMap = new HashMap<>();
        messageMap.put("target", target);
        messageMap.put("event", event);
        try {
            String jsonMessage = objectMapper.writeValueAsString(messageMap);
            listeners.values().forEach(listener -> listener.accept(jsonMessage));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @PostMapping
    public Mono<Map<String, String>> onQuery(@RequestBody Map<String, String> body) {

        sendMessageToAllListeners(body.getOrDefault("text", "N/A"), "clicked");

        Map<String, String> response = new HashMap<>();
        response.put("message", "OK");
        return Mono.just(response);
    }
}