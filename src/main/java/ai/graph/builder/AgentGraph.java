package ai.graph.builder;

import ai.graph.annotation.AgentNode;
import ai.graph.checkpoint.Checkpointer;
import ai.graph.checkpoint.InMemoryCheckpointer;
import ai.graph.interrupt.Interrupt;
import ai.graph.interrupt.InterruptHandler;
import ai.graph.model.AgentState;
import ai.graph.model.ChatMessage;

import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Function;

/**
 * Builds and executes an agent graph.
 *
 * <p>Nodes are methods annotated with {@link AgentNode @AgentNode} on a provider object.
 * Edges are defined via {@code edge(source, target)} for linear flows or
 * {@code conditional(source, fn)} for dynamic routing.
 *
 * <p>Example — ReAct agent:
 * <pre>{@code
 * AgentGraph graph = AgentGraph.builder()
 *     .nodes(myAgent)                          // scans @AgentNode methods
 *     .edge("generate", "execute")
 *     .edge("execute", "observe")
 *     .conditional("observe", state ->
 *         state.scratchpad().containsKey("final") ? "end" : "generate")
 *     .build();
 *
 * AgentState result = graph.run(
 *     AgentState.builder().messages(List.of(new ChatMessage.UserMessage("hi"))).build(),
 *     "my-thread-id"
 * );
 * }</pre>
 */
public class AgentGraph {

    public static Builder builder() { return new Builder(); }

    // -------------------------------------------------------------------------
    // Graph structure
    // -------------------------------------------------------------------------

    private final Map<String, NodeMethod> nodes;
    private final Map<String, String> edges;               // node → next node
    private final Function<AgentState, String> conditional; // null = no conditional edge
    private final String startNode;
    private final Checkpointer checkpointer;
    private final InterruptHandler interruptHandler;
    private final int maxSteps;

    private AgentGraph(Builder b) {
        this.nodes = Collections.unmodifiableMap(b.nodes);
        this.edges = Collections.unmodifiableMap(b.edges);
        this.conditional = b.conditional;
        this.startNode = b.startNode;
        this.checkpointer = b.checkpointer != null ? b.checkpointer : new InMemoryCheckpointer();
        this.interruptHandler = b.interruptHandler != null ? b.interruptHandler : state -> state;
        this.maxSteps = b.maxSteps;
    }

    /**
     * Run the graph from the start node.
     *
     * @param initialState graph state
     * @param threadId     conversation thread id (used for checkpointing)
     * @return final state after graph completes or interrupts
     */
    public AgentState run(AgentState initialState, String threadId) {
        AgentState state = initialState;
        int steps = 0;

        // Resume from checkpoint if available
        AgentState checkpoint = checkpointer.load(threadId);
        if (checkpoint != null) {
            state = checkpoint;
        }

        String currentNode = state.nextStep() != null ? state.nextStep() : startNode;

        while (currentNode != null && steps < maxSteps) {
            NodeMethod node = nodes.get(currentNode);
            if (node == null) throw new IllegalArgumentException("Unknown node: " + currentNode);

            // Save checkpoint before executing each step
            state = state.builder().nextStep(currentNode).build();
            checkpointer.save(threadId, String.valueOf(steps), state);

            state = node.execute(state);

            // Handle interrupt
            if (state.interrupt()) {
                return state;
            }

            // Advance to next node
            String next = edges.get(currentNode);
            if (next == null && conditional != null) {
                next = conditional.apply(state);
            }

            currentNode = next;
            steps++;
        }

        // Clear checkpoint on completion
        checkpointer.clear(threadId);

        return state.builder().nextStep(null).build();
    }

    // -------------------------------------------------------------------------
    // Builder
    // -------------------------------------------------------------------------

    public static class Builder {
        private final Map<String, NodeMethod> nodes = new LinkedHashMap<>();
        private final Map<String, String> edges = new LinkedHashMap<>();
        private Function<AgentState, String> conditional;
        private String startNode;
        private Checkpointer checkpointer;
        private InterruptHandler interruptHandler;
        private int maxSteps = AgentState.DEFAULT_MAX_STEPS;

        /**
         * Register nodes from an object whose methods are annotated with {@link AgentNode}.
         * The object's class is used to determine if a method is annotated.
         */
        public Builder nodes(Object provider) {
            for (Method method : provider.getClass().getDeclaredMethods()) {
                AgentNode ann = method.getAnnotation(AgentNode.class);
                if (ann != null) {
                    method.setAccessible(true);
                    String name = ann.value();
                    if (nodes.containsKey(name)) {
                        throw new IllegalArgumentException("Duplicate node name: " + name);
                    }
                    nodes.put(name, new NodeMethod(provider, method, ann.description()));
                    if (startNode == null) startNode = name;
                }
            }
            return this;
        }

        /**
         * Add a linear edge from source → target.
         */
        public Builder edge(String source, String target) {
            edges.put(source, target);
            return this;
        }

        /**
         * Route to a node determined by the given function.
         * The function is consulted when no linear edge exists for the current node.
         */
        public Builder conditional(String sourceNode, Function<AgentState, String> router) {
            if (!nodes.containsKey(sourceNode)) {
                throw new IllegalArgumentException("Conditional source node not registered: " + sourceNode);
            }
            this.conditional = router;
            return this;
        }

        public Builder checkpointer(Checkpointer checkpointer) {
            this.checkpointer = checkpointer;
            return this;
        }

        public Builder interruptHandler(InterruptHandler handler) {
            this.interruptHandler = handler;
            return this;
        }

        public Builder maxSteps(int maxSteps) {
            this.maxSteps = maxSteps;
            return this;
        }

        public AgentGraph build() {
            if (nodes.isEmpty()) throw new IllegalStateException("No @AgentNode methods registered");
            if (startNode == null && conditional == null) {
                throw new IllegalStateException("No start node and no conditional router defined");
            }
            return new AgentGraph(this);
        }
    }

    // -------------------------------------------------------------------------
    // NodeMethod
    // -------------------------------------------------------------------------

    private static class NodeMethod {
        private final Object provider;
        private final Method method;
        private final String description;

        NodeMethod(Object provider, Method method, String description) {
            this.provider = provider;
            this.method = method;
            this.description = description;
        }

        AgentState execute(AgentState state) {
            try {
                Object result = method.invoke(provider, state);
                if (result instanceof AgentState s) return s;
                if (result == null) return state;
                throw new IllegalStateException(
                    "@AgentNode method must return AgentState, got: " + result.getClass());
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException("Failed to invoke node: " + method.getName(), e);
            }
        }
    }
}