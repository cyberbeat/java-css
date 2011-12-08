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

package com.sun.stylesheet.swing;

import java.awt.Font;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.EventListener;
import java.util.HashMap;
import java.util.Map;

import org.jdesktop.beansbinding.ELProperty;
import org.jdesktop.beansbinding.PropertyStateListener;

import com.sun.stylesheet.PseudoclassEvent;
import com.sun.stylesheet.PseudoclassListener;
import com.sun.stylesheet.Styleable;
import com.sun.stylesheet.StylesheetException;
import com.sun.stylesheet.UnsupportedPropertyException;
import com.sun.stylesheet.styleable.DefaultPropertyHandler;
import com.sun.stylesheet.styleable.DefaultStyleable;
import com.sun.stylesheet.styleable.PropertyHandler;
import com.sun.stylesheet.styleable.StyleSupport;
import com.sun.stylesheet.types.Size;
import com.sun.stylesheet.types.TypeManager;

/**
 * Provides basic <code>Styleable</code> support for all instances of a
 * JavaBeans class. Java Beans introspection is used to determine the object's
 * supported properties, and features which cannot be deduced automatically
 * (such as finding parents and children) are stubbed out.
 * <p>
 * This class does not itself implement <code>Styleable</code>; it is instead
 * used by the generic wrapper class {@link DefaultStyleable}.
 * 
 *@author Ethan Nicholas
 */
public class BeanStyleSupport implements StyleSupport {
	/** The class that this handler provides support for. */
	private Class beanClass;

	/** The BeanInfo for the beanClass. */
	protected BeanInfo beanInfo;

	/** All matching Java classes. */
	private Class[] classes;

	/** Maps property names to their respective PropertyHandlers. */
	protected Map<String, PropertyHandler> properties;

	/** Maps EL expressions to ELProperties. */
	private Map<String, ELProperty> elProperties;

	/** Maps PseudoclassListeners to their PropertyStateListener adapters. */
	private Map<PseudoclassListener, EventListener> propertyStateListeners;

	/**
	 * Creates a new <code>BeanStyleSupport</code> which provides support for
	 * the specified class.
	 * 
	 *@param beanClass
	 *            the class which this handler supports
	 */
	public BeanStyleSupport(Class beanClass) {
		this.beanClass = beanClass;
	}

	/** Performs introspection on the beanClass and stores the results. */
	protected void init() throws IntrospectionException {
		if (beanInfo == null) {
			// perform introspection & cache the results
			beanInfo = getBeanInfo(beanClass);

			PropertyDescriptor[] propertiesArray = beanInfo.getPropertyDescriptors();
			properties = new HashMap<String, PropertyHandler>();
			for (int i = propertiesArray.length - 1; i >= 0; i--)
				createPropertyHandler(propertiesArray[i]);

			Class[] interfaces = beanClass.getInterfaces();
			classes = new Class[interfaces.length + 1];
			classes[0] = beanClass;
			System.arraycopy(interfaces, 0, classes, 1, interfaces.length);
		}
	}

	protected void createPropertyHandler(PropertyDescriptor descriptor) {
		String name = descriptor.getName();
		properties.put(name, new DefaultPropertyHandler(descriptor));
		if (descriptor.getPropertyType() == Font.class) {
			properties.put(name + "Size", new FontSizeHandler(descriptor));
			properties.put(name + "Family", new FontFamilyHandler(descriptor));
			properties.put(name + "Style", new FontStyleHandler(descriptor));
			properties.put(name + "Weight", new FontWeightHandler(descriptor));
		}
	}

	/**
	 * Returns the class which this <code>BeanStyleSupport</code> object
	 * supports.
	 */
	public Class getBeanClass() {
		return beanClass;
	}

	/**
	 * Returns the <code>BeanInfo</code> for the class which this
	 * <code>BeanStyleSupport</code> supports.
	 */
	public BeanInfo getBeanInfo() {
		try {
			init();
		} catch (IntrospectionException e) {
			throw new RuntimeException(e);
		}
		return beanInfo;
	}

	/**
	 * Returns the <code>BeanInfo</code> for the specified class.
	 * 
	 *@param beanClass
	 *            the bean class for which to retrieve <code>BeanInfo</code>
	 *@return the class' <code>BeanInfo</code>
	 */
	public static BeanInfo getBeanInfo(Class beanClass) throws IntrospectionException {
		return Introspector.getBeanInfo(beanClass);
	}

	/**
	 * Returns the type of the named property. This is the return type of the
	 * property's <code>get</code> method; for instance <code>JLabel</code>'s
	 * <code>text</code> property is a <code>String</code>.
	 * 
	 *@param object
	 *            the object being examined
	 *@param propertyName
	 *            the simple JavaBeans-style name of the property
	 *@return the property's type
	 *@throws CompilerException
	 *             if the type cannot be determined
	 */
	public Class getPropertyType(Object object, String propertyName) throws StylesheetException {
		try {
			init();
		} catch (IntrospectionException e) {
			throw new RuntimeException(e);
		}

		PropertyHandler property = properties.get(propertyName);
		if (property != null)
			return property.getPropertyType(object);
		else
			throw new UnsupportedPropertyException("property '" + propertyName + "' not found in "
				+ object);
	}

	/**
	 * Returns the value of a property for the specified object.
	 * 
	 *@param object
	 *            the object being queried
	 *@param propertyName
	 *            the property name
	 *@return the property's value
	 *@throws StylesheetException
	 *             if the property does not exist or could not be read
	 *@see #setProperty
	 */
	public Object getProperty(Object object, String propertyName) throws StylesheetException {
		try {
			init();
		} catch (IntrospectionException e) {
			throw new RuntimeException(e);
		}

		PropertyHandler property = properties.get(propertyName);
		if (property != null)
			return property.getProperty(object);
		else
			throw new UnsupportedPropertyException("property '" + propertyName
				+ "' could not be found in class " + getBeanClass().getName());
	}

	/**
	 * Sets the value of a property for the specified object.
	 * 
	 *@param object
	 *            the object being modified
	 *@param propertyName
	 *            the property name
	 *@param value
	 *            the property's new value
	 *@throws StylesheetException
	 *             if the property does not exist or could not be written
	 *@see #getProperty
	 */
	public void setProperty(Object object, String propertyName, Object value)
			throws StylesheetException {
		try {
			init();
		} catch (IntrospectionException e) {
			throw new RuntimeException(e);
		}

		PropertyHandler property = properties.get(propertyName);
		if (property != null)
			property.setProperty(object, value);
		else
			throw new UnsupportedPropertyException("property '" + propertyName
				+ "' could not be found in class " + getBeanClass().getName());
	}

	public PropertyHandler getPropertyHandler(String name) {
		return properties.get(name);
	}

	/**
	 * Returns <code>true</code> if the specified property should be inherited
	 * when found on a parent object.
	 */
	public boolean isPropertyInherited(Object object, String property)
			throws UnsupportedPropertyException {
		return property.startsWith("font") || property.equals("foreground")
			|| property.equals("enabled");
	}

	// returns EventListener rather than PropertyStateListener because Beans
	// Binding (and thus PropertyStateListener) is optional -- by not having it
	// in the method signature, the classload and thus requirement can be
	// delayed until the method is actually called
	private synchronized EventListener getPropertyStateListener(PseudoclassListener listener,
			PseudoclassEvent event) {
		if (propertyStateListeners == null)
			propertyStateListeners = new HashMap<PseudoclassListener, EventListener>();
		EventListener result = propertyStateListeners.get(listener);
		if (result == null) {
			result = new PseudoclassPropertyStateListener(listener, event);
			propertyStateListeners.put(listener, result);
		}
		return result;
	}

	private synchronized ELProperty getELProperty(String expression) {
		if (elProperties == null)
			elProperties = new HashMap<String, ELProperty>();
		ELProperty result = elProperties.get(expression);
		if (result == null) {
			result = ELProperty.create(expression);
			elProperties.put(expression, result);
		}
		return result;
	}

	private String transformPseudoclass(String pseudoclass) {
		if (pseudoclass.equals("enabled"))
			pseudoclass = "{enabled}";
		else if (pseudoclass.equals("disabled"))
			pseudoclass = "{!enabled}";
		else if (pseudoclass.equals("selected"))
			pseudoclass = "{selected}";
		else if (pseudoclass.equals("deselected"))
			pseudoclass = "{!selected}";
		return pseudoclass;
	}

	/**
	 * Adds a listener which will be notified as the specified pseudoclass is
	 * added to or removed from this object. For example,
	 * {@link java.awt.Component Components} support the <code>mouseover</code>
	 * pseudoclass, and adding a <code>PseudoclassListener</code> with the
	 * <code>mouseover</code> pseudoclass will cause it to receive notifications
	 * as the mouse enters and exits the component.
	 * 
	 *@param styleable
	 *            object to which the listener is being added
	 *@param pseudoclass
	 *            the pseudoclass to listen for
	 *@param listener
	 *            the listener which will receive pseudoclass notifications
	 *@throws StylesheetException
	 *             if the specified pseudoclass is not supported by this object
	 *@see #removePseudoclassListener
	 */
	public void addPseudoclassListener(DefaultStyleable styleable, String pseudoclass,
			final PseudoclassListener listener) throws StylesheetException {
		final PseudoclassEvent event = new PseudoclassEvent(styleable, pseudoclass);
		Object object = styleable.getBaseObject();

		pseudoclass = transformPseudoclass(pseudoclass);

		// add listeners
		if (pseudoclass.startsWith("{")) {
			ELProperty property = getELProperty("$" + pseudoclass);
			if (Boolean.TRUE.equals(property.getValue(object)))
				listener.pseudoclassAdded(event);
			property.addPropertyStateListener(object,
				(PropertyStateListener) getPropertyStateListener(listener, event));
		} else
			throw new StylesheetException(beanClass + " does not support pseudoclass "
				+ pseudoclass);
	}

	/**
	 * Removes a pseudoclass listener from this object.
	 * 
	 *@param styleable
	 *            object from which the listener is being removed
	 *@param pseudoclass
	 *            the pseudoclass which was being listened for
	 *@param listener
	 *            the listener to remove
	 *@throws StylesheetException
	 *             if the specified pseudoclass is not supported by this object
	 *@see #addPseudoclassListener
	 */
	public void removePseudoclassListener(DefaultStyleable styleable, String pseudoclass,
			final PseudoclassListener listener) throws StylesheetException {
		Object object = styleable.getBaseObject();

		pseudoclass = transformPseudoclass(pseudoclass);

		// remove listeners
		if (pseudoclass.startsWith("{")) {
			ELProperty property = getELProperty("$" + pseudoclass);
			property.removePropertyStateListener(object,
				(PropertyStateListener) getPropertyStateListener(listener, null));
		} else
			throw new StylesheetException(beanClass + " does not support pseudoclass "
				+ pseudoclass);
	}

	/**
	 * Maps string values onto integers, so that int-valued enumeration
	 * properties can be specified by strings. For example, when passed a key of
	 * 'alignment', this method should normally map the values 'left', 'center',
	 * and 'right' onto SwingConstants.LEFT, SwingConstants.CENTER, and
	 * SwingConstants.RIGHT respectively.
	 * <p>
	 * You do not normally need to call this method yourself; it is invoked by
	 * {@link #convertFromString} when an int-valued property has a value which
	 * is not a valid number. By default, this method looks at the
	 * <code>enumerationValues</code> value of the
	 * <code>PropertyDescriptor</code>.
	 * 
	 *@param key
	 *            the name of the int-typed property
	 *@param value
	 *            the non-numeric value that was specified for the property
	 *@throws IllegalArgumentException
	 *             if the property is an enumeration, but the value is not valid
	 *@throws NumberFormatException
	 *             if the property is not an enumeration
	 */
	protected int constantValue(String key, String value) {
		BeanInfo beanInfo = getBeanInfo();
		PropertyDescriptor[] properties = beanInfo.getPropertyDescriptors();
		String lowercaseValue = value.toLowerCase();
		for (int i = 0; i < properties.length; i++) {
			if (properties[i].getName().equals(key)) {
				Object[] values = (Object[]) properties[i].getValue("enumerationValues");
				if (values != null) {
					for (int j = 0; j < values.length - 2; j += 3) {
						if (((String) values[j]).toLowerCase().equals(lowercaseValue))
							return ((Integer) values[j + 1]).intValue();
					}

					StringBuffer message = new StringBuffer("value of '" + key
						+ "' must be one of: [");
					for (int j = 0; j < values.length - 2; j += 3) {
						if (j != 0)
							message.append(", ");
						message.append(((String) values[j]).toLowerCase());
					}
					message.append("] (found '" + value + "')");
					throw new IllegalArgumentException(message.toString());
				}
			}
		}
		throw new NumberFormatException(value);
	}

	/**
	 * As {@link TypeManager#convertFromString(String, Class)}, except that it
	 * additionally supports simple constant names for <code>int</code>-valued
	 * types.
	 * 
	 *@param key
	 *            the name of the property whose value is being converted
	 *@param value
	 *            the raw string value of the property as it appears in the XML
	 *@param type
	 *            the datatype to convert the string into
	 *@see #constantValue
	 */
	protected Object convertFromString(String key, String value, Class type) {
		try {
			return TypeManager.convertFromString(value, type);
		} catch (NumberFormatException e) {
			if (type == int.class || type == Integer.class)
				return new Integer(constantValue(key, value));
			else
				throw e;
		}
	}

	/**
	 * Returns the object's parent. The default implementation returns
	 * <code>null</code>.
	 */
	public Styleable getStyleableParent(Object object) {
		return null;
	}

	/**
	 * Returns the object's children. The default implementation returns
	 * <code>null</code>.
	 */
	public Styleable[] getStyleableChildren(Object object) {
		return null;
	}

	/**
	 * Returns the object's ID. The default implementation returns
	 * <code>null</code>.
	 */
	public String getID(Object object) {
		return null;
	}

	/**
	 * Returns an array of all Java classes which should be considered matches
	 * for the object. The default implementation returns the base object class
	 * plus all implemented interfaces.
	 */
	public Class[] getObjectClasses(Object object) {
		try {
			init();
		} catch (IntrospectionException e) {
			throw new RuntimeException(e);
		}

		return classes;
	}

	/**
	 * Returns the object's style class. The default implementation returns
	 * <code>null</code>.
	 */
	public String getStyleClass(Object object) {
		return null;
	}

	public void addHierarchyListener(DefaultStyleable object) {
	}

	public Map<String, Object> splitCompoundProperty(Object object, String property, Object value)
			throws StylesheetException {
		Class type = getPropertyType(object, property);
		if (type == Font.class) { // todo: make this pluggable
			Font font = (Font) value;
			Map<String, Object> result = new HashMap<String, Object>();
			result.put(property + "Family", font.getFamily());
			result.put(property + "Size", new Size(font.getSize2D(), Size.Unit.PT));
			result.put(property + "Weight",
				(font.getStyle() & Font.BOLD) != 0 ? FontWeightHandler.FontWeight.BOLD
						: FontWeightHandler.FontWeight.NORMAL);
			result.put(property + "Style",
				(font.getStyle() & Font.ITALIC) != 0 ? FontStyleHandler.FontStyle.ITALIC
						: FontStyleHandler.FontStyle.PLAIN);
			return result;
		}
		return null;
	}

	/**
	 * Returns a string representation of this object.
	 */
	public String toString() {
		return getClass().getName() + "[" + getBeanClass().getName() + "]";
	}
}