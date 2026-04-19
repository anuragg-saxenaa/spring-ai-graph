package ai.graph.annotation;

import java.lang.annotation.*;

/**
 * Marks a method as an agent graph node.
 *
 * <p>Methods annotated with {@code @AgentNode} must accept {@link ai.graph.model.AgentState}
 * as their first parameter and return {@link ai.graph.model.AgentState} or {@code void}
 * (when using a {@code nodeName} to mutate state in-place).
 *
 * <p>Example:
 * <pre>{@code
 * @AgentNode("generate")
 * public AgentState generate(AgentState state) {
 *     // produce next state
 * }
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AgentNode {

    /**
     * The name of this node. Must be unique within the graph.
     */
    String value();

    /**
     * Description of what this node does. Used for tracing and debugging.
     */
    String description() default "";
}