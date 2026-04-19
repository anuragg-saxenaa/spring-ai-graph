package ai.graph.interrupt;

import java.lang.annotation.*;

/**
 * Marks a node as a human-in-the-loop interrupt point.
 *
 * <p>When a node annotated with {@code @Interrupt} is reached, the graph pauses
 * and {@link InterruptHandler#pause(ai.graph.model.AgentState)} is called.
 * The caller must invoke {@link InterruptHandler#resume(ai.graph.model.AgentState)}
 * to continue execution.
 *
 * <p>Example:
 * <pre>{@code
 * @Interrupt(value = "approval", description = "Await human approval before executing")
 * public AgentState approvalGate(AgentState state) {
 *     // state.interrupt = true is set automatically
 *     return state;
 * }
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Interrupt {

    /**
     * Identifier for this interrupt point.
     */
    String value();

    /**
     * Human-readable description of what is being paused.
     */
    String description() default "";
}