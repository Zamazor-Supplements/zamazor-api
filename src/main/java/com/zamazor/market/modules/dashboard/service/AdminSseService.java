package com.zamazor.market.modules.dashboard.service;

import com.zamazor.market.modules.dashboard.events.AdminEvent;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AdminSseService {
	private final Set<SseEmitter> emitters = ConcurrentHashMap.newKeySet();

	public SseEmitter connect() {
		SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);

		emitters.add(emitter);

		emitter.onCompletion(() -> emitters.remove(emitter));
		emitter.onTimeout(() -> emitters.remove(emitter));
		emitter.onError((ex) -> emitters.remove(emitter));

		try {
			emitter.send(
					SseEmitter.event()
							.name("CONNECTED")
							.data(Map.of("message", "Connected"))
			);
		} catch (Exception e) {
			emitters.remove(emitter);
		}
		return emitter;
	}

	public void publish(AdminEvent event) {
		for (SseEmitter emitter : emitters) {
			try {
				emitter.send(
						SseEmitter.event()
								.id(event.id())
								.name(event.type())
								.data(event.data())
				);
			} catch (Exception e) {
				emitters.remove(emitter);
			}
		}
	}

	public void heartbeat() {
		for (SseEmitter emitter : emitters) {
			try {
				emitter.send(
						SseEmitter.event()
								.comment("heartbeat")
				);
			} catch (Exception e) {
				emitters.remove(emitter);
			}
		}
	}
}
