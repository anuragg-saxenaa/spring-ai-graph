package ai.graph.example;

import ai.graph.annotation.AgentNode;
import ai.graph.builder.AgentGraph;
import ai.graph.model.AgentState;
import ai.graph.model.ChatMessage;

/**
 * Minimal ReAct (Reason + Act + Observe) agent using the graph primitives.
 *
 * Node order: generate → execute → observe
 *
 * The observe node decides whether to loop back to generate or terminate.
 * Tool results are appended as ToolMessage to the conversation.
 */
public class ReActAgent {

    private final Object toolExecutor;
    private final AgentGraph graph;

    public ReActAgent(Object toolExecutor, String modelName) {
        this.toolExecutor = toolExecutor;

        ReActNodes nodes = new ReActNodes(modelName);

        this.graph = AgentGraph.builder()
                .nodes(nodes)
                .edge("generate", "execute")
                .edge("execute", "observe")
                .conditional("observe", state -> {
                    // If last tool result contains "final" keyword, we're done
                    if (!state.messages().isEmpty()) {
                        var last = state.messages().get(state.messages().size() - 1);
                        if (last instanceof ChatMessage.ToolMessage tm
                            && tm.text().toLowerCase().contains("final")) {
                            return null; // end
                        }
                    }
                    return "generate"; // loop
                })
                .maxSteps(20)
                .build();
    }

    public AgentGraph getGraph() { return graph; }

    /**
     * Run the agent with an initial user message.
     */
    public AgentState run(String userMessage) {
        var state = AgentState.builder()
                .messages(java.util.List.of(new ChatMessage.UserMessage(userMessage)))
                .scratchpad(java.util.Map.of("step", 0))
                .build();
        return graph.run(state, "default-thread");
    }

    // -------------------------------------------------------------------------
    // Node implementations — scanned by AgentGraph builder via @AgentNode
    // -------------------------------------------------------------------------

    public class ReActNodes {
        private final String modelName;

        public ReActNodes(String modelName) { this.modelName = modelName; }

        @AgentNode(value = "generate", description = "Ask the LLM to decide the next tool call")
        public AgentState generate(AgentState state) {
            // In a real implementation this would call the LLM with the
            // full conversation + scratchpad and return an AssistantMessage
            // with toolCalls set.
            int step = (int) state.scratchpad().getOrDefault("step", 0);
            var newState = state.builder()
                    .putScratchpad("step", step + 1)
                    .addMessage(new ChatMessage.AssistantMessage(
                        "[generate node] Would call LLM here with: " + state.messages()))
                    .build();
            return newState;
        }

        @AgentNode(value = "execute", description = "Execute the chosen tool and append result")
        public AgentState execute(AgentState state) {
            var newState = state.builder()
                    .addMessage(new ChatMessage.ToolMessage(
                        "tool-call-1",
                        "[execute node] Would run tool here, append ToolMessage"))
                    .build();
            return newState;
        }

        @AgentNode(value = "observe", description = "Check tool result, decide next step")
        public AgentState observe(AgentState state) {
            return state; // conditional() in the graph decides routing
        }
    }
}