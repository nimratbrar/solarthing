package me.retrodaredevil.solarthing.actions;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import me.retrodaredevil.action.Action;
import me.retrodaredevil.solarthing.actions.environment.ActionEnvironment;

import java.time.Duration;

@JsonTypeName("waitms")
public class WaitMillisActionNode implements ActionNode {
	private final long wait;

	public WaitMillisActionNode(@JsonProperty("wait") long wait) {
		this.wait = wait;
	}

	@Override
	public Action createAction(ActionEnvironment actionEnvironment) {
		return new WaitAction(Duration.ofMillis(wait));
	}
}
