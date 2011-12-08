/*
 * Copyright 2008 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

/* Modified by Volker Härtel, 8 Dec 2011 */ package com.sun.stylesheet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import com.sun.stylesheet.types.Size;

/**
 * Used by {@link Rule Rules} to apply properties to styleable objects.
 * <code>PropertyManager</code> provides automatic support for priorities as
 * well as property removal, and so for these features to work correctly all
 * modifications to {@link Styleable} objects should be made through
 * <code>PropertyManager</code>.
 * 
 *@author Ethan Nicholas
 */
public class PropertyManager {
	private static final String NO_STYLE = "no style";

	private static final boolean debug = false;

	private static boolean animationSupport;
	static {
		try {
			Class.forName("org.jdesktop.animation.timing.Animator");
			animationSupport = true;
		} catch (ClassNotFoundException e) {
			animationSupport = false;
		}
	}

	/**
	 * Maps styleables to their property values. Property values are stored in a
	 * Map, and each "value" is actually a list of all applicable values sorted
	 * by priority.
	 */
	public static Map<Styleable, Map<String, PropertyList>> properties = new WeakHashMap<Styleable, Map<String, PropertyList>>();

	/**
	 * Maps styleables to their pseudoclass listeners.
	 */
	public static Map<Styleable, List<PseudoclassValue>> pseudoclasses = new WeakHashMap<Styleable, List<PseudoclassValue>>();

	private static class PropertyList extends ArrayList<PropertyValue> {
		private boolean overridden; // true if someone has modified this
		// property outside of CSS' control
	}

	/**
	 * Encapsulates a property value in effect for a Styleable. Each property
	 * value stores its priority for use in determining which one should
	 * currently be in effect, and its stylesheet to help manage stylesheet
	 * updates.
	 */
	public static class PropertyValue implements Comparable {
		private String property;

		private Object value;

		private Priority priority;

		private Rule source;

		private Animation animation;

		private boolean inherited;

		public PropertyValue(String property, Object value, Rule source, Priority priority,
				Animation animation, boolean inherited) {
			this.property = property;
			this.value = value;
			this.priority = priority;
			this.source = source;
			this.animation = animation;
			this.inherited = inherited;
		}

		public String getPropertyName() {
			return property;
		}

		public Object getValue() {
			return value;
		}

		public Priority getPriority() {
			return priority;
		}

		public Rule getSource() {
			return source;
		}

		public Animation getAnimation() {
			return animation;
		}

		public boolean wasInherited() {
			return inherited;
		}

		public int compareTo(Object o) {
			return getPriority().compareTo(((PropertyValue) o).getPriority());
		}

		public boolean equals(Object o) {
			// we explicitly do not compare the values -- if two properties
			// came from the same stylesheet with the same priority, they're
			// from the same rule. By not comparing the values, we're free
			// to recreate them as needed without worrying about identity.
			if (!(o instanceof PropertyValue))
				return false;
			PropertyValue value = (PropertyValue) o;
			if (!value.getPriority().equals(getPriority()))
				return false;
			if (!property.equals(value.getPropertyName()))
				return false;
			if (value.inherited != inherited)
				return false;
			return source == value.getSource();
		}

		public int hashCode() {
			return property.hashCode() ^ (source != null ? source.hashCode() : 0)
				^ (value != null ? value.hashCode() : 0) ^ priority.hashCode();
		}

		public String toString() {
			return "PropertyValue[" + property + "=" + value + ", " + priority + "]";
		}
	}

	private static class PseudoclassValue {
		private String pseudoclass;

		private PseudoclassListener listener;

		private Rule source;

		public PseudoclassValue(String pseudoclass, PseudoclassListener listener, Rule source) {
			this.pseudoclass = pseudoclass;
			this.listener = listener;
			this.source = source;
		}

		public String getPseudoclass() {
			return pseudoclass;
		}

		public PseudoclassListener getPseudoclassListener() {
			return listener;
		}

		public Rule getSource() {
			return source;
		}

		public boolean equals(Object o) {
			if (!(o instanceof PseudoclassValue))
				return false;
			PseudoclassValue p = (PseudoclassValue) o;
			return pseudoclass.equals(p.pseudoclass) && listener == p.listener
				&& source == p.source;
		}

		public int hashCode() {
			return pseudoclass.hashCode() ^ listener.hashCode() ^ source.hashCode();
		}
	}

	private PropertyManager() { /* not instantiable */
	}

	/**
	 * Returns an unsorted list of all properties currently applied to the
	 * specified object (this includes values which are being masked by
	 * higher-priority values).
	 */
	public static List<PropertyValue> getAllPropertiesForObject(Styleable object) {
		Map<String, PropertyList> propertyMap = properties.get(object);
		if (propertyMap == null) {
			propertyMap = new HashMap<String, PropertyList>();
			properties.put(object, propertyMap);
		}

		List<PropertyValue> result = new ArrayList<PropertyValue>();
		for (List<PropertyValue> list : propertyMap.values())
			result.addAll(list);
		return result;
	}

	/**
	 * Returns a sorted list of all values for the specified property currently
	 * applied to an object. The highest-priority value is at the end of the
	 * list; this is the value which should actually take effect.
	 */
	public static List<PropertyValue> getPropertyListForObject(Styleable object, String property) {
		Map<String, PropertyList> propertyMap = properties.get(object);
		if (propertyMap == null) {
			propertyMap = new HashMap<String, PropertyList>();
			properties.put(object, propertyMap);
		}

		PropertyList propertyList = propertyMap.get(property);
		if (propertyList == null) {
			propertyList = new PropertyList();
			propertyMap.put(property, propertyList);
		}

		return propertyList;
	}

	/**
	 * Returns true if there is a matching value currently applied to the
	 * object.
	 */
	private static boolean isPropertyApplied(Styleable object, String property, Rule source,
			Priority priority, boolean wasInherited) {
		List<PropertyValue> propertyList = getPropertyListForObject(object, property);
		for (int i = 0; i < propertyList.size(); i++) {
			PropertyValue p = propertyList.get(i);
			if (p.getSource() == source)
				return true;
		}
		return false;
	}

	/** Adds the specified value to the property list for the object. */
	private static void propertyApplied(Styleable object, String property, Object value,
			Rule source, Priority priority, Animation animation, boolean wasInherited) {
		PropertyList propertyList = (PropertyList) getPropertyListForObject(object, property);
		propertyList.overridden = false;
		propertyList.add(new PropertyValue(property, value, source, priority, animation,
				wasInherited));
		Collections.sort(propertyList);
	}

	/** Removes the specifiedfied value from the property list for the object. */
	private static void propertyRemoved(Styleable object, String property, Object value,
			Rule source, Priority priority, Animation animation, boolean wasInherited) {
		PropertyList propertyList = (PropertyList) getPropertyListForObject(object, property);
		if (propertyList.overridden == false
			&& (!animationSupport || !AnimationManager.isAnimating(object, property))) {
			Object value1 = object.getProperty(property);
			Object value2 = getCurrentValue(object, property);
			boolean equal;
			if (value1 == null)
				equal = value2 == null;
			else
				equal = value1.equals(value2);
			if (!equal) { // property has been changed outside of our control
				if (debug)
					System.err.println("WARNING: property " + property + " of " + object
						+ " has been overridden (" + value1 + " != " + value2 + ")");
				propertyList.overridden = true;
			}
		}
		propertyList.remove(new PropertyValue(property, value, source, priority, animation,
				wasInherited));
	}

	/**
	 * Returns the highest-priority value currently in effect for the specified
	 * property.
	 */
	private static Object getCurrentValue(Styleable object, String property) {
		PropertyList propertyList = (PropertyList) getPropertyListForObject(object, property);
		if (propertyList.overridden)
			return object.getProperty(property);
		if (propertyList.size() > 0)
			return propertyList.get(propertyList.size() - 1).getValue();
		return NO_STYLE;
	}

	/**
	 * Applies inherited values to an object's children. Has no effect if the
	 * values have already been inherited.
	 * 
	 *@param object
	 *            the object which should pass inherited values to its children
	 *@throws StylesheetException
	 *             if an error occurs
	 */
	public static void cascadeFrom(Styleable object) throws StylesheetException {
		Styleable[] children = object.getStyleableChildren();
		if (children != null) {
			for (Styleable child : children)
				cascadeTo(child, true);
		}
	}

	/**
	 * Applies inherited values from the object's parent. Has no effect if the
	 * values have already been inherited.
	 * 
	 *@param object
	 *            the object which should inherit values from its parent
	 *@param recurse
	 *            true to recursively call <code>cascadeTo</code> on the
	 *            object's children
	 *@throws StylesheetException
	 *             if an error occurs
	 */
	public static void cascadeTo(Styleable object, boolean recurse) throws StylesheetException {
		Styleable parent = object.getStyleableParent();
		if (parent != null) {
			List<PropertyValue> propertyList = getAllPropertiesForObject(parent);
			Set<PropertyValue> cascaded = null;
			for (PropertyValue property : propertyList) {
				if (property.source != null
					&& object.isPropertyInherited(property.getPropertyName())) {
					Priority oldPriority = property.getPriority();
					Priority newPriority = (Priority) oldPriority.clone();
					newPriority.setDepth(oldPriority.getDepth() + 1);
					applyProperty(object, property.getPropertyName(), property.getValue(), property
						.getSource(), newPriority, property.getAnimation(), true);
					if (cascaded == null)
						cascaded = new HashSet<PropertyValue>();
					cascaded.add(property);
				}
			}

			// remove defunct cascaded properties
			propertyList = getAllPropertiesForObject(object);
			for (PropertyValue property : propertyList) {
				if (property.wasInherited()) {
					boolean matched = false;
					if (cascaded != null) {
						for (PropertyValue cascadedProperty : cascaded) {
							if (cascadedProperty.getPropertyName().equals(
								property.getPropertyName())
								&& cascadedProperty.getSource() == property.getSource())
								matched = true;
						}
					}
					if (!matched) {
						removeProperty(object, property.getPropertyName(), property.getValue(),
							property.getSource(), property.getPriority(), property.getAnimation(),
							true);
					}
				}
			}
		}

		if (recurse) {
			for (Styleable child : object.getStyleableChildren())
				cascadeTo(child, true);
		}
	}

	/**
	 * Returns a list of all pseudoclass listeners currently applied to an
	 * object.
	 */
	private static List<PseudoclassValue> getPseudoclassListForObject(Styleable object) {
		List<PseudoclassValue> pseudoclassList = pseudoclasses.get(object);
		if (pseudoclassList == null) {
			pseudoclassList = new ArrayList<PseudoclassValue>();
			pseudoclasses.put(object, pseudoclassList);
		}

		return pseudoclassList;
	}

	/**
	 * Adds a pseudoclass listener to a styleable object.
	 * 
	 *@param object
	 *            the object to listen to
	 *@param pseudoclass
	 *            the name of the pseudoclass to listen for
	 *@param listener
	 *            the listener which should be notified when the pseudoclass is
	 *            added or removed
	 *@param source
	 *            the source applying the listener
	 *@throws StylesheetException
	 *             if the pseudoclass is unsupported
	 */
	public static void addPseudoclassListener(Styleable object, String pseudoclass,
			PseudoclassListener listener, Rule source) throws StylesheetException {
		List<PseudoclassValue> list = getPseudoclassListForObject(object);
		list.add(new PseudoclassValue(pseudoclass, listener, source));
		object.addPseudoclassListener(pseudoclass, listener);
	}

	/**
	 * Removes a pseudoclass listener from a styleable object. Has no effect if
	 * the listener is not present.
	 * 
	 *@param object
	 *            the object to which the listener was attached
	 *@param pseudoclass
	 *            the name of the pseudoclass
	 *@param listener
	 *            the listener to remove
	 *@param source
	 *            the source which applied the listener
	 *@throws StylesheetException
	 *             if the pseudoclass is unsupported
	 */
	public static void removePseudoclassListener(Styleable object, String pseudoclass,
			PseudoclassListener listener, Rule source) throws StylesheetException {
		List<PseudoclassValue> list = getPseudoclassListForObject(object);
		list.remove(new PseudoclassValue(pseudoclass, listener, source));
		object.removePseudoclassListener(pseudoclass, listener);
	}

	/**
	 * Removes all properties and pseudoclass listeners in effect for the
	 * specified object and all of its descendents.
	 * 
	 *@param object
	 *            the object from which to remove styles
	 *@throws StylesheetException
	 *             if an error occurs
	 */
	public static void removeAllStyles(Styleable object) throws StylesheetException {
		List<PropertyValue> propertyList = getAllPropertiesForObject(object);
		for (PropertyValue property : propertyList) {
			if (property.getSource() != null) {
				removeProperty(object, property.getPropertyName(), property.getValue(), property
					.getSource(), property.getPriority(), property.getAnimation());
			}
		}
		List<PseudoclassValue> pseudoclassList = new ArrayList<PseudoclassValue>(
				getPseudoclassListForObject(object));
		for (PseudoclassValue pseudoclass : pseudoclassList) {
			removePseudoclassListener(object, pseudoclass.getPseudoclass(), pseudoclass
				.getPseudoclassListener(), pseudoclass.getSource());
		}
		for (Styleable child : object.getStyleableChildren())
			removeAllStyles(child);
	}

	/**
	 * Removes all properties applied by a given stylesheet from the specified
	 * object and all of its descendents.
	 * 
	 *@param stylesheet
	 *            the stylesheet whose properties should be removed
	 *@param object
	 *            the object from which properties should be removed
	 *@throws StylesheetException
	 *             if an error occurs
	 */
	public static void removeStylesheet(Stylesheet stylesheet, Styleable object)
			throws StylesheetException {
		List<PropertyValue> propertyList = getAllPropertiesForObject(object);
		for (PropertyValue property : propertyList) {
			if (property.getSource() != null && property.getSource().getStylesheet() == stylesheet) {
				removeProperty(object, property.getPropertyName(), property.getValue(), property
					.getSource(), property.getPriority(), property.getAnimation(), property
					.wasInherited());
			}
		}
		List<PseudoclassValue> pseudoclassList = new ArrayList<PseudoclassValue>(
				getPseudoclassListForObject(object));
		for (PseudoclassValue pseudoclass : pseudoclassList) {
			if (pseudoclass.getSource() != null
				&& pseudoclass.getSource().getStylesheet() == stylesheet) {
				removePseudoclassListener(object, pseudoclass.getPseudoclass(), pseudoclass
					.getPseudoclassListener(), pseudoclass.getSource());
			}
		}
		for (Styleable child : object.getStyleableChildren())
			removeStylesheet(stylesheet, child);
	}

	/**
	 * Applies a property value to an object. The new value will only be visible
	 * if it is currently the highest-priority value in effect for the given
	 * property. For example, if you assign a high-priority green foreground and
	 * a low-priority red foreground to a given object, the object's foreground
	 * will be green. If you later remove the green foreground, the foreground
	 * will change to red. Removing both foreground values will cause the
	 * foreground to revert to its original value.
	 * 
	 *@param object
	 *            the object to which the property should be assigned
	 *@param property
	 *            the name of the property to affect, e.g. "foreground"
	 *@param newValue
	 *            the new value of the property
	 *@param source
	 *            the source applying this value
	 *@param priority
	 *            the value's priority
	 *@see #removeProperty
	 */
	public static void applyProperty(Styleable object, String property, Object newValue,
			Rule source, Priority priority, Animation animation) throws StylesheetException {
		applyProperty(object, property, newValue, source, priority, animation, false);
	}

	private static void applyProperty(Styleable object, String property, Object newValue,
			Rule source, Priority priority, Animation animation, boolean wasInherited)
			throws StylesheetException {
		if (newValue instanceof Size && !((Size) newValue).isComputed())
			throw new IllegalArgumentException("must compute size '" + newValue
				+ "' before applying");
		Map<String, Object> split = object.splitCompoundProperty(property, newValue);
		if (split != null) {
			for (Map.Entry<String, Object> e : split.entrySet())
				applyProperty(object, e.getKey(), e.getValue(), source, priority, animation,
					wasInherited);
		} else if (!isPropertyApplied(object, property, source, priority, wasInherited)) {
			Object value = getCurrentValue(object, property);
			if (value == NO_STYLE) {
				if (animationSupport && AnimationManager.isAnimating(object, property))
					value = AnimationManager.getTargetValue(object, property);
				else
					value = object.getProperty(property);
				propertyApplied(object, property, value, null, new Priority(false, -1, -1, -1, -1,
						-1, -1, -1), null, false);
			}
			propertyApplied(object, property, newValue, source, priority, animation, wasInherited);
			if (animation != null && animationSupport)
				AnimationManager.animateTransition(object, property, newValue, animation);
			else
				object.setProperty(property, getCurrentValue(object, property));
		}
	}

	/**
	 * Removes a property value from an object. Has no effect if the property
	 * was not actually in effect, and if a higher-priority value was masking
	 * the removed value the visible property value will not actually change.
	 * 
	 *@param object
	 *            the object from which the property should be removed
	 *@param property
	 *            the name of the property to affect, e.g. "foreground"
	 *@param oldValue
	 *            the value to remove
	 *@param source
	 *            the source which applied this value
	 *@param priority
	 *            the value's priority
	 *@see #applyProperty
	 */
	public static void removeProperty(Styleable object, String property, Object oldValue,
			Rule source, Priority priority, Animation animation) throws StylesheetException {
		removeProperty(object, property, oldValue, source, priority, animation, false);
	}

	private static void removeProperty(Styleable object, String property, Object oldValue,
			Rule source, Priority priority, Animation animation, boolean wasInherited)
			throws StylesheetException {
		Map<String, Object> split = object.splitCompoundProperty(property, oldValue);
		if (split != null) {
			for (Map.Entry<String, Object> e : split.entrySet())
				removeProperty(object, e.getKey(), e.getValue(), source, priority, animation,
					wasInherited);
		}
		if (isPropertyApplied(object, property, source, priority, wasInherited)) {
			propertyRemoved(object, property, oldValue, source, priority, animation, wasInherited);
			Object value = getCurrentValue(object, property);
			if (value == NO_STYLE)
				throw new java.lang.IllegalStateException("found unexpected NO_STYLE value");
			if (animation != null && animationSupport)
				AnimationManager.animateTransition(object, property, getCurrentValue(object,
					property), animation);
			else
				object.setProperty(property, getCurrentValue(object, property));

			Map<String, PropertyList> propertyMap = properties.get(object);
			if (propertyMap != null) {
				PropertyList propertyList = propertyMap.get(property);
				if (propertyList.size() == 1)
					propertyMap.remove(property); // no styles left
			}
		} else if (debug)
			System.err.println("WARNING: attempted to remove property " + object + "." + property
				+ " (" + oldValue + "), but it was " + "not present");
	}

	static void makeStatic(Stylesheet stylesheet, Styleable object) {
		// get list of all highest-priority properties in effect
		Map<String, PropertyList> propertyMap = properties.get(object);
		if (propertyMap == null)
			return;

		List<PropertyValue> properties = new ArrayList<PropertyValue>();
		for (List<PropertyValue> list : propertyMap.values()) {
			// grab the last (highest-priority) value for each property
			properties.add(list.get(list.size() - 1));
		}

		for (PropertyValue property : properties) {
			if (property.getSource() != null && property.getSource().getStylesheet() == stylesheet) {
				// highest-priority value was applied by a static stylesheet,
				// so remove ALL values in effect for this property
				List<PropertyValue> allValues = new ArrayList(getPropertyListForObject(object,
					property.getPropertyName()));
				for (PropertyValue p : allValues) {
					removeProperty(object, p.getPropertyName(), p.getValue(), p.getSource(), p
						.getPriority(), p.getAnimation(), p.wasInherited());
				}
				// reapply value statically
				object.setProperty(property.getPropertyName(), property.getValue());
			}
		}
		for (Styleable child : object.getStyleableChildren())
			makeStatic(stylesheet, child);
	}
}