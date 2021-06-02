package io.tlf.jme.jfx;

import com.jme3.input.event.InputEvent;
import com.jme3.input.event.KeyInputEvent;
import com.jme3.input.event.MouseButtonEvent;
import javafx.scene.Node;
import javafx.scene.control.TextInputControl;

import java.util.Map;
import java.util.function.Function;

/**
 * The Functions determines, if a certain Input event is allowed to be
 * set as 'consumed' by the JmeFXInput Listener.
 * @author Klaus Pichler
 *
 */
public class JmeFxEventConsumeAllowedFunction implements Function<InputEvent, Boolean> {

	private final Map<Integer, KeyInputEvent> jmeKeyInputEvents;
	private final Map<Integer, MouseButtonEvent> jmeMouseButtonEvents;

	public JmeFxEventConsumeAllowedFunction(JmeMemoryInputHandler inputHandler) {
		jmeKeyInputEvents = inputHandler.getJmeKeyInputEvents();
		jmeMouseButtonEvents = inputHandler.getJmeMouseButtonEvents();
	}

	@Override
	public Boolean apply(InputEvent event) {

		if(event instanceof MouseButtonEvent) {
			return checkMouseEvent((MouseButtonEvent) event);
		}

		if(event instanceof KeyInputEvent) {
			return checkKeyEvent((KeyInputEvent) event);
		}


		return true;
	}

	private Boolean checkMouseEvent(MouseButtonEvent event) {

		if(event.isReleased()) {
			if(jmeMouseButtonEvents.get(event.getButtonIndex())!=null) {
				//Mouse Button was pressed by JME. FX is not allowed to consume the event.
				return false;
			}
		}


		return true;
	}

	private Boolean checkKeyEvent(KeyInputEvent event) {

		if(event.isReleased()) {
			if(jmeKeyInputEvents.get(event.getKeyCode())!=null) {
				//Key was pressed by JME. FX is not allowed to consume the event.
				return false;
			}
		}

		Node node = JavaFxUI.getInstance().getScene().focusOwnerProperty().get();
		if (node instanceof TextInputControl && node.isVisible()) {
			return true;
		} else {
			return false;
		}
	}

}
