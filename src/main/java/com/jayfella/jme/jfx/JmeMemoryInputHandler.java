package com.jayfella.jme.jfx;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jme3.input.RawInputListener;
import com.jme3.input.event.JoyAxisEvent;
import com.jme3.input.event.JoyButtonEvent;
import com.jme3.input.event.KeyInputEvent;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;
import com.jme3.input.event.TouchEvent;

/**
 * This {@link RawInputListener} will be executed after the
 * FX Input Listener. So pressed Keys and mouse buttons 
 * can be considered as JME Pressed Input.
 * 
 * @author Klaus Pichler
 *
 */
public class JmeMemoryInputHandler implements RawInputListener {

	private static final Logger log = LoggerFactory.getLogger(JmeMemoryInputHandler.class);
	
	private final Map<Integer, KeyInputEvent> jmeKeyInputEvents = new HashMap<>();
	private final Map<Integer, MouseButtonEvent> jmeMouseButtonEvents = new HashMap<>();
	
	@Override
	public void beginInput() {
		// Nothing to do
	}

	@Override
	public void endInput() {
		// Nothing to do
	}

	@Override
	public void onJoyAxisEvent(JoyAxisEvent evt) {
		// Nothing to do	
	}

	@Override
	public void onJoyButtonEvent(JoyButtonEvent evt) {
		// Nothing to do
	}

	@Override
	public void onMouseMotionEvent(MouseMotionEvent evt) {
		// Nothing to do	
	}

	@Override
	public void onMouseButtonEvent(MouseButtonEvent evt) {
		
		// Storing or removing now JME Mouse button events	
		MouseButtonEvent existingEvent = jmeMouseButtonEvents.get(evt.getButtonIndex());

		if (evt.isPressed()) {
			if (existingEvent == null) {
				jmeMouseButtonEvents.put(evt.getButtonIndex(), evt);
				log.debug("JME - MouseEvent ADDED: " + evt.toString() + " - SIZE: " + jmeMouseButtonEvents.size());
			}
		}

		else if (evt.isReleased()) {
			if (existingEvent != null) {
				if (jmeMouseButtonEvents.remove(evt.getButtonIndex()) != null) {
					log.debug("JME - MouseEvent REMOVED: " + evt.toString() + " - SIZE: "
							+ jmeMouseButtonEvents.size());
				}
			}
		}		
	}

	@Override
	public void onKeyEvent(KeyInputEvent evt) {
		
		// Storing or removing now JME key pressed events.	
		if (evt.getKeyCode() == 0) {
			return;
		}

		KeyInputEvent existingEvent = jmeKeyInputEvents.get(evt.getKeyCode());

		if (evt.isPressed()) {
			if (existingEvent == null) {
				jmeKeyInputEvents.put(evt.getKeyCode(), evt);
				log.debug("JME - KeyEvent ADDED: " + evt.toString() + " - SIZE: " + jmeKeyInputEvents.size());
			}
		}

		else if (evt.isReleased() && !evt.isRepeating()) {
			if (existingEvent != null) {
				if (jmeKeyInputEvents.remove(evt.getKeyCode()) != null) {
					log.debug("JME - KeyEvent REMOVED: " + evt.toString() + " - SIZE: " + jmeKeyInputEvents.size());
				}
			}
		}
		
	}

	@Override
	public void onTouchEvent(TouchEvent evt) {
		// Nothing to do
		
	}

	public Map<Integer, KeyInputEvent> getJmeKeyInputEvents() {
		return jmeKeyInputEvents;
	}

	public Map<Integer, MouseButtonEvent> getJmeMouseButtonEvents() {
		return jmeMouseButtonEvents;
	}

}
