# spring-ai-graph

Agent orchestration layer for Spring AI — graph primitives, interrupt, and checkpointing.

Inspired by LangGraph (Python/JS). Provides a directed graph of `@AgentNode` methods with conditional routing, checkpointing, and human-in-the-loop interruption.

## Status

MVP implementation of the primitives described in [spring-projects/spring-ai#5826](https://github.com/spring-projects/spring-ai/issues/5826).

## Primitives

| Primitive | File | Description |
|-----------|------|-------------|
| `AgentState` | `model/AgentState.java` | Record carrying messages, scratchpad, next step, interrupt flag |
| `@AgentNode` | `annotation/AgentNode.java` | Marks a method as a graph node |
| `AgentGraph` | `builder/AgentGraph.java` | Builds and runs the graph |
| `Checkpointer` | `checkpoint/Checkpointer.java` | Persists state between turns |
| `InMemoryCheckpointer` | `checkpoint/InMemoryCheckpointer.java` | Simple in-memory implementation |
| `InterruptHandler` | `interrupt/InterruptHandler.java` | HiTL pause/resume callbacks |
| `@Interrupt` | `interrupt/Interrupt.java` | Marks an interrupt point (pending) |

## Usage

### Define nodes

```java
public class MyAgent {
    private final ChatModel chatModel;

    @AgentNode("generate")
    public AgentState generate(AgentState state) {
        // Call LLM, append AssistantMessage to state.messages
    }

    @AgentNode("execute")
    public AgentState execute(AgentState state) {
        // Execute tool, append ToolMessage
    }

    @AgentNode("observe")
    public AgentState observe(AgentState state) {
        // Decide next step based on scratchpad
        return state;
    }
}
```

### Build and run

```java
MyAgent agent = new MyAgent();

AgentGraph graph = AgentGraph.builder()
        .nodes(agent)
        .edge("generate", "execute")
        .edge("execute", "observe")
        .conditional("observe", state ->
                state.scratchpad().containsKey("done") ? null : "generate")
        .checkpointer(new InMemoryCheckpointer())
        .maxSteps(20)
        .build();

AgentState result = graph.run(
        AgentState.builder()
                .messages(List.of(new ChatMessage.UserMessage("What's the weather?")))
                .build(),
        "thread-42"
);
```

### Checkpointing

Use `JdbcCheckpointer` (coming) for production multi-instance deployments:

```java
AgentGraph graph = AgentGraph.builder()
        .nodes(myAgent)
        .checkpointer(new JdbcCheckpointer(dataSource))
        .build();
```

### Human-in-the-loop

```java
AgentGraph graph = AgentGraph.builder()
        .nodes(myAgent)
        .interruptHandler(new InterruptHandler() {
            @Override
            public AgentState pause(AgentState state) {
                // Show state to user, wait for approval
                System.out.println("Paused at: " + state.nextStep());
                return state; // resume unchanged
            }
        })
        .build();
```

### Plan-Execute pattern

```java
// See ai.graph.example.PlanExecuteAgent for a full implementation.
// Sketch:
//
// planner()     → produces plan in scratchpad
// executor()     → executes one step of plan, updates scratchpad
// replanner()    → checks completion, loops or ends
```

## Building

Requires Java 21+.

```bash
./mvnw compile
./mvnw test
```

## Contributing

This is an MVP targeting the Spring AI 2.x ecosystem. See the [upstream issue](https://github.com/spring-projects/spring-ai/issues/5826) for the full feature spec.