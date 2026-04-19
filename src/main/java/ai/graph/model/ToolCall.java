package ai.graph.model;

/**
 * Represents a tool call from the LLM.
 */
public record ToolCall(
    String id,
    String name,
    String arguments
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String id;
        private String name;
        private String arguments;

        public Builder id(String id) { this.id = id; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder arguments(String arguments) { this.arguments = arguments; return this; }

        public ToolCall build() {
            return new ToolCall(id, name, arguments);
        }
    }
}