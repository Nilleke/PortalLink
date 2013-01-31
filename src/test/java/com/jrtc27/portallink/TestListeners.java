package com.jrtc27.portallink;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.junit.Test;

import junit.framework.Assert;

/**
 * Ensure we aren't missing any {@link org.bukkit.event.EventHandler} annotations, and that all listeners implement {@link org.bukkit.event.Listener}
 *
 * @author Feildmaster, jrtc27
 */
public class TestListeners {
	private Class[] knownListeners = new Class[] { PortalLinkListener.class };

	@Test
	public void testAllKnown() {
		for (Class clazz : this.knownListeners) {
			testListener(clazz);
		}
	}

	/**
	 * Test a class which should implement {@link org.bukkit.event.Listener} and contain {@link org.bukkit.event.EventHandler} annotations
	 *
	 * @author Feildmaster
	 */
	private void testListener(Class clazz) {
		// Assert the class is even a listener
		Assert.assertTrue("Class: " + clazz.getSimpleName() + " does not implement Listener!", Listener.class.isAssignableFrom(clazz));

		for (Method method : clazz.getDeclaredMethods()) {
			// We only care about public functions.
			if (!Modifier.isPublic(method.getModifiers())) continue;
			// Don't mess with non-void
			if (!Void.TYPE.equals(method.getReturnType())) continue;
			// Only look for functions with 1 parameter
			if (method.getParameterTypes().length != 1) continue;

			// This is an event function...
			if (Event.class.isAssignableFrom(method.getParameterTypes()[0])) {
				// Make sure @EventHandler is present!
				Assert.assertTrue(method.getName() + " is missing @EventHandler!", method.isAnnotationPresent(EventHandler.class));
			}
		}
	}
}
