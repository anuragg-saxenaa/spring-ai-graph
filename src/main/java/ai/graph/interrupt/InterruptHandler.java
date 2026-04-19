package ai.graph.interrupt;

import ai.graph.model.AgentState;

/**
 * Handler called when the graph reaches an {@link Interrupt} point.
 *
 * <p>Implement this to integrate with your UI / approval workflow.
 *
 * <p>Usage with AgentGraph:
 * <pre>{@code
 * AgentGraph graph = AgentGraph.builder()
 *     .interruptHandler(myHandler)
 *     // ...
 *     .build();
 * }</pre>
 */
@FunctionalInterface
public interface InterruptHandler {

    /**
     * Called when the graph reaches an interrupt point.
     *
     * @param state the graph state at interrupt time
     * @return the state to resume with (may be modified by the human)
     */
    AgentState pause(AgentState state);

    /**
     * Resume the graph with the given state.
     * Default implementation returns the state unchanged.
     */
    default AgentState resume(AgentState state) {
        return state;
    }
}