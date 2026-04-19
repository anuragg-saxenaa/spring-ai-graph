package ai.graph.model;

import java.util.List;
import java.util.Map;

/**
 * Represents a message in the agent conversation graph.
 */
public sealed interface ChatMessage
        permits ChatMessage.SystemMessage,
                ChatMessage.UserMessage,
                ChatMessage.AssistantMessage,
                ChatMessage.ToolMessage {

    String text();

    Map<String, Object> metadata();

    final class SystemMessage implements ChatMessage {
        private final String text;
        private final Map<String, Object> metadata;

        public SystemMessage(String text, Map<String, Object> metadata) {
            this.text = text;
            this.metadata = metadata != null ? metadata : Map.of();
        }

        public SystemMessage(String text) { this(text, null); }

        @Override public String text() { return text; }
        @Override public Map<String, Object> metadata() { return metadata; }
    }

    final class UserMessage implements ChatMessage {
        private final String text;
        private final List<String> images;
        private final Map<String, Object> metadata;

        public UserMessage(String text, List<String> images, Map<String, Object> metadata) {
            this.text = text;
            this.images = images != null ? images : List.of();
            this.metadata = metadata != null ? metadata : Map.of();
        }

        public UserMessage(String text) { this(text, null, null); }

        @Override public String text() { return text; }
        public List<String> images() { return images; }
        @Override public Map<String, Object> metadata() { return metadata; }
    }

    final class AssistantMessage implements ChatMessage {
        private final String text;
        private final List<ai.graph.model.ToolCall> toolCalls;
        private final String toolCallId;
        private final Map<String, Object> metadata;

        public AssistantMessage(String text, List<ai.graph.model.ToolCall> toolCalls,
                                String toolCallId, Map<String, Object> metadata) {
            this.text = text;
            this.toolCalls = toolCalls != null ? toolCalls : List.of();
            this.toolCallId = toolCallId;
            this.metadata = metadata != null ? metadata : Map.of();
        }

        public AssistantMessage(String text) { this(text, null, null, null); }

        @Override public String text() { return text; }
        public List<ai.graph.model.ToolCall> toolCalls() { return toolCalls; }
        public String toolCallId() { return toolCallId; }
        @Override public Map<String, Object> metadata() { return metadata; }
    }

    final class ToolMessage implements ChatMessage {
        private final String toolCallId;
        private final String text;
        private final Map<String, Object> metadata;

        public ToolMessage(String toolCallId, String text, Map<String, Object> metadata) {
            this.toolCallId = toolCallId;
            this.text = text;
            this.metadata = metadata != null ? metadata : Map.of();
        }

        public ToolMessage(String toolCallId, String text) { this(toolCallId, text, null); }

        @Override public String text() { return text; }
        public String toolCallId() { return toolCallId; }
        @Override public Map<String, Object> metadata() { return metadata; }
    }
}