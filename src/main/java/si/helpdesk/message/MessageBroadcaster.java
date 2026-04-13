package si.helpdesk.message;

import io.smallrye.mutiny.subscription.MultiEmitter;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class MessageBroadcaster {

    private final Map<Long, List<MultiEmitter<? super MessageDTO>>> emitters = new ConcurrentHashMap<>();

    public void register(Long conversationId, MultiEmitter<? super MessageDTO> emitter) {
        emitters.computeIfAbsent(conversationId, k -> new ArrayList<>()).add(emitter);
        emitter.onTermination(() -> unregister(conversationId, emitter));
    }

    public void broadcast(Long conversationId, MessageDTO message) {
        List<MultiEmitter<? super MessageDTO>> list = emitters.get(conversationId);
        if (list != null) {
            List<MultiEmitter<? super MessageDTO>> dead = new ArrayList<>();
            for (MultiEmitter<? super MessageDTO> emitter : list) {
                if (!emitter.isCancelled()) {
                    emitter.emit(message);
                } else {
                    dead.add(emitter);
                }
            }
            list.removeAll(dead);
        }
    }

    public void unregister(Long conversationId, MultiEmitter<? super MessageDTO> emitter) {
        List<MultiEmitter<? super MessageDTO>> list = emitters.get(conversationId);
        if (list != null) {
            list.remove(emitter);
        }
    }
}
