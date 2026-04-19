package ai.graph.checkpoint;

import ai.graph.model.AgentState;

/**
 * Persists agent state between turns, enabling resume after interrupt or crash.
 */
public interface Checkpointer {

    /**
     * Save the current graph state.
     *
     * @param threadId  conversation thread identifier
     * @param checkpointId unique checkpoint identifier (e.g. turn number)
     * @param state     the state to persist
     */
    void save(String threadId, String checkpointId, AgentState state);

    /**
     * Load the most recent checkpoint for a thread, if any.
     *
     * @param threadId conversation thread identifier
     * @return the last saved state, or null if none exists
     */
    AgentState load(String threadId);

    /**
     * Delete all checkpoints for a thread.
     */
    void clear(String threadId);
}