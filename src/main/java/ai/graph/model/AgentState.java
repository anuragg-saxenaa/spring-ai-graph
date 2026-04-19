package ai.graph.model;

/**
 * State carried through the agent graph execution.
 *
 * @param messages accumulated conversation messages
 * @param scratchpad working memory for the current turn
 * @param nextStep  the next node to execute (null = end)
 * @param interrupt whether execution has paused for human feedback
 */
public record AgentState(
    java.util.List<ai.graph.model.ChatMessage> messages,
    java.util.Map<String, Object> scratchpad,
    String nextStep,
    boolean interrupt
) {
    public static final int DEFAULT_MAX_STEPS = 50;

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private java.util.List<ai.graph.model.ChatMessage> messages = new java.util.ArrayList<>();
        private java.util.Map<String, Object> scratchpad = new java.util.HashMap<>();
        private String nextStep;
        private boolean interrupt = false;

        public Builder messages(java.util.List<ai.graph.model.ChatMessage> messages) {
            this.messages = new java.util.ArrayList<>(messages);
            return this;
        }

        public Builder addMessage(ai.graph.model.ChatMessage message) {
            this.messages.add(message);
            return this;
        }

        public Builder scratchpad(java.util.Map<String, Object> scratchpad) {
            this.scratchpad = new java.util.HashMap<>(scratchpad);
            return this;
        }

        public Builder putScratchpad(String key, Object value) {
            this.scratchpad.put(key, value);
            return this;
        }

        public Builder nextStep(String nextStep) {
            this.nextStep = nextStep;
            return this;
        }

        public Builder interrupt(boolean interrupt) {
            this.interrupt = interrupt;
            return this;
        }

        public AgentState build() {
            return new AgentState(java.util.Collections.unmodifiableList(this.messages),
                                  java.util.Collections.unmodifiableMap(this.scratchpad),
                                  this.nextStep,
                                  this.interrupt);
        }
    }
}