package ai.graph.checkpoint;

import ai.graph.model.AgentState;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * In-memory checkpointer. Not suitable for multi-instance deployments.
 * Use {@link JdbcCheckpointer} for production.
 */
public class InMemoryCheckpointer implements Checkpointer {

    private final Map<String, AgentState> store = new ConcurrentHashMap<>();

    @Override
    public void save(String threadId, String checkpointId, AgentState state) {
        // threadId → latest state
        store.put(threadId, state);
    }

    @Override
    public AgentState load(String threadId) {
        return store.get(threadId);
    }

    @Override
    public void clear(String threadId) {
        store.remove(threadId);
    }
}