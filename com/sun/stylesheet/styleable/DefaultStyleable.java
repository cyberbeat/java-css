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

/* Modified by Volker Härtel, 8 Dec 2011 */ 

package com.sun.stylesheet.styleable;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.sun.stylesheet.PropertyManager;
import com.sun.stylesheet.PseudoclassListener;
import com.sun.stylesheet.Styleable;
import com.sun.stylesheet.Stylesheet;
import com.sun.stylesheet.StylesheetException;
import com.sun.stylesheet.types.TypeManager;

/**
 * Default <code>Styleable</code> wrapper which supports a variety of classes.
 * <code>DefaultStyleable</code> can wrap any class which is supported by an
 * implementation of {@link StyleSupport}.
 */
public class DefaultStyleable implements Styleable {
	private static class StylesheetApplication {
		private Stylesheet stylesheet;

		private int depth;

		private StylesheetApplication(Stylesheet stylesheet, int depth) {
			this.stylesheet = stylesheet;
			this.depth = depth;
		}

		public boolean equals(Object o) {
			if (!(o instanceof StylesheetApplication))
				return false;

			StylesheetApplication s = (StylesheetApplication) o;
			return s.stylesheet == stylesheet && s.depth == depth;
		}

		public int hashCode() {
			return stylesheet.hashCode() ^ depth;
		}
	}

	// static Map<Class, StyleSupport> support = new HashMap<Class,
	// StyleSupport>();

	Object object;

	Set<StylesheetApplication> stylesheets = new HashSet<StylesheetApplication>();

	public DefaultStyleable(Object object) {
		this.object = object;
		getStyleSupport().addHierarchyListener(this);
	}

	/**
	 * Informs the object that a new stylesheet is being applied to it. The
	 * <code>depth</code> parameter indicates the "ancestry level" of the style
	 * root (the object to which the stylesheet is being directly applied); for
	 * instance if the style root is this object's parent, the depth is 1. The
	 * depth is needed to correctly resolve priorities in the face of
	 * conflicting rules, with lower depths having precedence over higher
	 * depths.
	 * <p>
	 * Knowing which stylesheets are affecting it allows
	 * <code>DefaultStyleable</code> to automatically apply stylesheets to
	 * newly-added children, for objects which provide the necessary
	 * notifications.
	 * 
	 *@param s
	 *            the stylesheet being applied
	 *@param depth
	 *            the depth of this object below the style root
	 *@see #removeStylesheet
	 */
	public void addStylesheet(Stylesheet s, int depth) {
		stylesheets.add(new StylesheetApplication(s, depth));
	}

	/**
	 * Informs the object that a stylesheet no longer applies to it. The
	 * <code>depth</code> parameter indicates the "ancestry level" of the style
	 * root (the object to which the stylesheet is being directly applied); for
	 * instance if the style root is this object's parent, the depth is 1. The
	 * depth is needed to correctly resolve priorities in the face of
	 * conflicting rules, with lower depths having precedence over higher
	 * depths.
	 * <p>
	 * Knowing which stylesheets are affecting it allows
	 * <code>DefaultStyleable</code> to automatically apply stylesheets to
	 * newly-added children, for objects which provide the necessary
	 * notifications.
	 * 
	 *@param s
	 *            the stylesheet being removed
	 *@param depth
	 *            the depth of this object below the style root
	 *@see #addStylesheet
	 */
	public void removeStylesheet(Stylesheet s, int depth) {
		stylesheets.remove(new StylesheetApplication(s, depth));
	}

	private synchronized StyleSupport getStyleSupport() {
		return TypeManager.getStyleSupport(object);
	}

	public String getID() {
		return getStyleSupport().getID(object);
	}

	public Object getBaseObject() {
		return object;
	}

	public Class[] getObjectClasses() {
		return getStyleSupport().getObjectClasses(object);
	}

	public String getStyleClass() {
		return getStyleSupport().getStyleClass(object);
	}

	public Object convertPropertyFromString(String propertyName, String value)
			throws StylesheetException {
		Class type = getStyleSupport().getPropertyType(object, propertyName);
		return TypeManager.convertFromString(value, type);
	}

	public Object getProperty(String key) throws StylesheetException {
		return getStyleSupport().getProperty(object, key);
	}

	public void setProperty(String key, Object value) throws StylesheetException {
		getStyleSupport().setProperty(object, key, value);
	}

	public Styleable getStyleableParent() {
		return getStyleSupport().getStyleableParent(object);
	}

	public Styleable[] getStyleableChildren() {
		return getStyleSupport().getStyleableChildren(object);
	}

	public boolean isPropertyInherited(String propertyName) throws StylesheetException {
		return getStyleSupport().isPropertyInherited(object, propertyName);
	}

	public void addPseudoclassListener(String pseudoclass, PseudoclassListener listener)
			throws StylesheetException {
		getStyleSupport().addPseudoclassListener(this, pseudoclass, listener);
	}

	public void removePseudoclassListener(String pseudoclass, PseudoclassListener listener)
			throws StylesheetException {
		getStyleSupport().removePseudoclassListener(this, pseudoclass, listener);
	}

	public void childAdded(Styleable child) {
		try {
			for (StylesheetApplication s : stylesheets)
				s.stylesheet.applyTo(child, s.depth + 1);
			PropertyManager.cascadeTo(child, true);
		} catch (StylesheetException ex) {
			throw new RuntimeException(ex);
		}
	}

	public void childRemoved(Styleable child) {
		try {
			PropertyManager.removeAllStyles(child);
		} catch (StylesheetException ex) {
			throw new RuntimeException(ex);
		}
	}

	public Map<String, Object> splitCompoundProperty(String property, Object value)
			throws StylesheetException {
		return getStyleSupport().splitCompoundProperty(object, property, value);
	}

	public boolean equals(Object o) {
		if (!(o instanceof DefaultStyleable))
			return false;
		return object == ((DefaultStyleable) o).object;
	}

	public int hashCode() {
		return object.hashCode();
	}

	public String toString() {
		return "DefaultStyleable[" + object.getClass().getName() + ", "
			+ System.identityHashCode(object) + ", " + System.identityHashCode(this) + "]";
	}
}