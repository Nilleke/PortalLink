/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jrtc27.portallink;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import junit.framework.Assert;

import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.junit.Test;

/**
 * Ensure we aren't missing any {@link org.bukkit.event.EventHandler} annotations, and that all listeners implement {@link org.bukkit.event.Listener}
 *
 * @author Feildmaster, jrtc27
 */
public class TestListeners {
	private Class[] knownListeners = new Class[] { PlayerListener.class };

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
