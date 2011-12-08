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

/* Modified by Volker Härtel, 8 Dec 2011 */ package com.sun.stylesheet.types;

import java.awt.AWTPermission;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Window;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import javax.swing.AbstractButton;
import javax.swing.JLabel;
import javax.swing.KeyStroke;
import javax.swing.border.Border;

import com.sun.stylesheet.Styleable;
import com.sun.stylesheet.StylesheetException;
import com.sun.stylesheet.styleable.DefaultStyleable;
import com.sun.stylesheet.styleable.StyleSupport;
import com.sun.stylesheet.swing.AbstractButtonStyleSupport;
import com.sun.stylesheet.swing.BorderConverter;
import com.sun.stylesheet.swing.ComponentStyleSupport;
import com.sun.stylesheet.swing.DimensionConverter;
import com.sun.stylesheet.swing.FontConverter;
import com.sun.stylesheet.swing.InsetsConverter;
import com.sun.stylesheet.swing.JLabelStyleSupport;
import com.sun.stylesheet.swing.KeyStrokeConverter;
import com.sun.stylesheet.swing.TextDecorationConverter;
import com.sun.stylesheet.swing.TextDecorationHandler;
import com.sun.stylesheet.swing.WindowStyleSupport;

/**
 * Returns converters and wrappers for the various types supported by the CSS
 * engine. Additional types can be registered with
 * {@link #registerTypeConverter registerTypeConverter},
 * {@link #registerStyleableWrapper registerStyleableWrapper}, and
 * {@link #registerStyleSupport registerStyleSupport}.
 * 
 * @author Ethan Nicholas
 */
public class TypeManager {
	private static boolean initialized;

	// stores styleable wrappers so we can guarantee that the same wrapper is
	// always used for the same object
	public static Map<Object, WeakReference<Styleable>> styleables = new WeakHashMap<Object, WeakReference<Styleable>>();

	// *** IMPORTANT ***
	// Note that the converters and styleSupport maps are ClassMaps, which means
	// they automatically handle subclasses of the keys inserted into them.
	private static ClassMap<Object> converters = new ClassMap<Object>();

	private static Map<Class, TypeConverter> cachedConverters = new HashMap<Class, TypeConverter>();

	private static ClassMap<Interpolator> interpolators = new ClassMap<Interpolator>();

	private static ClassMap<Class<? extends Styleable>> wrappers = new ClassMap<Class<? extends Styleable>>();

	private static ClassMap<Class<? extends StyleSupport>> styleSupportClasses = new ClassMap<Class<? extends StyleSupport>>();

	private static Map<Class, StyleSupport> styleSupportInstances = new HashMap<Class, StyleSupport>();

	private static Map<Class, Constructor> objectConstructors = new HashMap<Class, Constructor>();

	private static Map<Class, Constructor> classConstructors = new HashMap<Class, Constructor>();

	private TypeManager() { /* not instantiable */
	}

	static {
		registerPrimitiveConverter(boolean.class);
		registerPrimitiveConverter(Boolean.class);
		registerPrimitiveConverter(byte.class);
		registerPrimitiveConverter(Byte.class);
		registerPrimitiveConverter(short.class);
		registerPrimitiveConverter(Short.class);
		registerPrimitiveConverter(int.class);
		registerPrimitiveConverter(Integer.class);
		registerPrimitiveConverter(long.class);
		registerPrimitiveConverter(Long.class);
		registerPrimitiveConverter(float.class);
		registerPrimitiveConverter(Float.class);
		registerPrimitiveConverter(double.class);
		registerPrimitiveConverter(Double.class);
		registerPrimitiveConverter(char.class);
		registerPrimitiveConverter(Character.class);
		registerPrimitiveConverter(String.class);

		registerTypeConverter(String.class, new StringConverter());
		registerTypeConverter(Size.class, new SizeConverter());

		try {
			registerTypeConverter(Time.class, new TimeConverter());
		} catch (NoClassDefFoundError e) {
		}

		registerTypeConverterClass(Enum.class, EnumConverter.class);

		try {
			registerTypeConverter(java.awt.Color.class, new com.sun.stylesheet.swing.ColorConverter());
			registerTypeConverter(Insets.class, new InsetsConverter());
			registerTypeConverter(Dimension.class, new DimensionConverter());
			registerTypeConverter(KeyStroke.class, new KeyStrokeConverter());
			registerTypeConverter(Border.class, new BorderConverter());
			registerTypeConverter(Font.class, new FontConverter());
			registerTypeConverter(TextDecorationHandler.Decoration.class, new TextDecorationConverter());
			registerInterpolator(Color.class, new com.sun.stylesheet.swing.ColorInterpolator());
			registerStyleSupport(Component.class, ComponentStyleSupport.class);
			registerStyleSupport(AbstractButton.class, AbstractButtonStyleSupport.class);
			registerStyleSupport(JLabel.class, JLabelStyleSupport.class);
			registerStyleSupport(Window.class, WindowStyleSupport.class);
		} catch (NoClassDefFoundError e) {
		}

		try {
			registerInterpolator(Integer.class, new IntInterpolator());
			registerInterpolator(Float.class, new FloatInterpolator());
			registerInterpolator(Double.class, new DoubleInterpolator());
			registerInterpolator(Size.class, new SizeInterpolator());
		} catch (NoClassDefFoundError e) {
		}

		registerStyleableWrapper(Object.class, DefaultStyleable.class);

		initialized = true;
	}

	private static void checkPermission() {
		if (initialized) {
			SecurityManager security = System.getSecurityManager();
			if (security != null)
				security.checkPermission(new AWTPermission("setGlobalStylesheet"));
		}
	}

	/**
	 * Registers a new <code>StyleSupport</code>, which allows
	 * {@link DefaultStyleable} to provide support for additional classes. The
	 * support class will be used whenever the specified class or any of its
	 * descendents (which are not registered with more specific support classes)
	 * is encountered.
	 * <p>
	 * The support class must have a constructor which takes a
	 * <code>Class</code>; this constructor will be invoked once for each
	 * specific class that requires a <code>StyleSupport</code>. For example
	 * {@link ComponentStyleSupport} is registered to <code>Component</code> by
	 * default. Each specific subclass of <code>Component</code> will receive
	 * its own instance of <code>ComponentStyleSupport</code>, created by
	 * passing the class to its constructor.
	 * <p>
	 * <code>StyleSupport</code> instances are used by
	 * <code>DefaultStyleable</code> to support specific classes. If a different
	 * wrapper class has been registered, the new wrapper may ignore registered
	 * <code>StyleSupport</code> classes. This call does not replace any
	 * <code>StyleSupport</code> instances which may already have been created.
	 * <p>
	 * This call requires the <code>setGlobalStylesheet</code> AWTPermission.
	 * 
	 * @param cls
	 *            the class to support
	 * @param support
	 *            the <code>StyleSupport</code> class which provides support for
	 *            the class
	 * @throws SecurityException
	 *             if the required permission is not available
	 */
	public static void registerStyleSupport(Class cls, Class<? extends StyleSupport> support) {
		checkPermission();
		styleSupportClasses.put(cls, support);
	}

	/**
	 * Registers a new <code>Styleable</code> wrapper, which allows the CSS
	 * engine to support additional classes. An instance of the wrapper class
	 * will be created whenever the specified class or any of its descendents
	 * (which are not registered with more specific wrapper classes) is
	 * encountered.
	 * <p>
	 * The wrapper class must have a constructor which takes an
	 * <code>Object</code>; this constructor will be invoked once for each
	 * specific object that requires a <code>Styleable</code> wrapper. For
	 * example {@link DefaultStyleable} is registered to <code>Object</code> by
	 * default. This means that, unless a more specific wrapper is registered,
	 * any instance of any class will have a <code>DefaultStyleable</code>
	 * constructed for it by passing the object to DefaultStyleable's
	 * constructor.
	 * <p>
	 * Internally, <code>DefaultStyleable</code> uses <code>StyleSupport</code>
	 * instances to provide support for specific classes. Generally developers
	 * will want to provide new <code>StyleSupport</code> classes using
	 * {@link #registerStyleSupport} rather than replace the
	 * <code>DefaultStyleable</code> altogether.
	 * <p>
	 * Wrappers are only created for classes which do not implement
	 * <code>Styleable</code>. This call does not replace any wrappers which may
	 * already have been created. A <code>StylesheetException</code> will be
	 * thrown if you attempt to register a wrapper for a class which implements
	 * <code>Styleable</code>.
	 * <p>
	 * This call requires the <code>setGlobalStylesheet</code> AWTPermission.
	 * 
	 * @param cls
	 *            the class to wrap
	 * @param wrapperClass
	 *            the <code>Styleable</code> class which wraps the class
	 * @throws SecurityException
	 *             if the required permission is not available
	 * @throws StylesheetException
	 *             if you attempt to register a wrapper for a
	 *             <code>Styleable</code>
	 * @see #registerStyleSupport
	 */
	public static void registerStyleableWrapper(Class cls, Class<? extends Styleable> wrapperClass) {
		checkPermission();
		if (Styleable.class.isAssignableFrom(cls))
			throw new StylesheetException("cannot register wrapper for Styleable " + cls);
		wrappers.put(cls, wrapperClass);
	}

	/**
	 * Returns a <code>Styleable</code> for the specified object. If the object
	 * implements <code>Styleable</code>, the object itself is returned,
	 * otherwise a wrapper is created for it. Wrappers are stable over time:
	 * repeated calls to this method for the same object will always return the
	 * same result.
	 * <p>
	 * By default the wrapper class is always <code>DefaultStyleable</code>,
	 * which obtains support for specific classes by calling
	 * {@link #getStyleSupport}. Additional <code>StyleSupport</code> classes
	 * may be registered with {@link #registerStyleSupport} and different
	 * wrappers may be registered with {@link #registerStyleableWrapper}.
	 * 
	 * @param object
	 *            the object to wrap
	 * @return a <code>Styleable</code> wrapper for the object
	 * @throws StylesheetException
	 *             if an error occurs creating the wrapper
	 * @see #registerStyleableWrapper
	 */
	public static Styleable getStyleable(Object object) throws StylesheetException {
		// System.out.println("get stylable for "+object);
		if (object instanceof Styleable)
			return (Styleable) object;
		else {
			WeakReference<Styleable> result = styleables.get(object);
			if (result == null) {
				Class<? extends Styleable> wrapper = null;
				wrapper = wrappers.get(object.getClass());
				if (wrapper == null) {
					Class[] interfaces = object.getClass().getInterfaces();
					for (int i = 0; i < interfaces.length; i++) {
						wrapper = wrappers.get(interfaces[i]);
						if (wrapper != null)
							break;
					}
				}
				if (wrapper == null)
					throw new StylesheetException("no registered Styleable wrapper " + "for " + object.getClass());
				try {
					Constructor c = getObjectConstructor(wrapper);
					result = new WeakReference<Styleable>((Styleable) c.newInstance(object));
					// System.out.println("create new stylable "+result);
					styleables.put(object, result);
				} catch (NoSuchMethodException e) {
					throw new StylesheetException("Styleable wrapper " + wrapper
							+ " does not have a public constructor which takes " + "Object");
				} catch (InstantiationException e) {
					throw new StylesheetException(e);
				} catch (IllegalAccessException e) {
					throw new StylesheetException(e);
				} catch (InvocationTargetException e) {
					throw new StylesheetException(e);
				}
			}
			return result.get();
		}
	}

	/**
	 * Returns the <code>StyleSupport</code> instance which
	 * {@link DefaultStyleable} should use for the specified object. Additional
	 * <code>StyleSupport</code> classes may be registered with
	 * {@link #registerStyleSupport}.
	 * 
	 * @param object
	 *            the object which needs a <code>StyleSupport</code> instance
	 * @return the registered <code>StyleSupport</code>
	 * @throws StylesheetException
	 *             if an error occurs creating the <code>StyleSupport</code>
	 * @see #registerStyleSupport
	 */
	public static StyleSupport getStyleSupport(Object object) throws StylesheetException {
		Class cls = object.getClass();
		StyleSupport result = styleSupportInstances.get(cls);
		if (result == null) {
			Class<? extends StyleSupport> styleSupportClass = styleSupportClasses.get(cls);
			if (styleSupportClass == null) {
				Class[] interfaces = object.getClass().getInterfaces();
				for (int i = 0; i < interfaces.length; i++) {
					styleSupportClass = styleSupportClasses.get(interfaces[i]);
					if (styleSupportClass != null)
						break;
				}
			}
			if (styleSupportClass == null)
				throw new StylesheetException("no registered StyleSupport for " + cls);
			try {
				Constructor c = getClassConstructor(styleSupportClass);
				result = (StyleSupport) c.newInstance(cls);
				styleSupportInstances.put(object.getClass(), result);
			} catch (NoSuchMethodException e) {
				throw new StylesheetException("Style support " + styleSupportClass
						+ " does not have a public constructor which takes " + "Class");
			} catch (InstantiationException e) {
				throw new StylesheetException(e);
			} catch (IllegalAccessException e) {
				throw new StylesheetException(e);
			} catch (InvocationTargetException e) {
				throw new StylesheetException(e);
			}
		}
		return result;
	}

	private static Constructor getObjectConstructor(Class cls) throws NoSuchMethodException {
		Constructor result = objectConstructors.get(cls);
		if (result == null) {
			result = cls.getConstructor(Object.class);
			objectConstructors.put(cls, result);
		}
		return result;
	}

	private static Constructor getClassConstructor(Class cls) throws NoSuchMethodException {
		Constructor result = classConstructors.get(cls);
		if (result == null) {
			result = cls.getConstructor(Class.class);
			classConstructors.put(cls, result);
		}
		return result;
	}

	private static void registerPrimitiveConverter(Class type) {
		registerTypeConverter(type, new PrimitiveConverter(type));
	}

	/**
	 * Registers a new <code>TypeConverter</code>, which is used to convert from
	 * the strings found in CSS files to the destination type. The
	 * <code>TypeConverter</code> will be called whenever the specified class or
	 * any of its descendents (which do not have a more specific
	 * <code>TypeConverter</code> registered) is required.
	 * <p>
	 * This call requires the <code>setGlobalStylesheet</code> AWTPermission.
	 * 
	 * @param type
	 *            the class to register
	 * @param converter
	 *            the <code>TypeConverter</code> which provides support for the
	 *            class
	 * @throws SecurityException
	 *             if the required permission is not available
	 */
	public static void registerTypeConverter(Class type, TypeConverter converter) {
		checkPermission();
		converters.put(type, converter);
	}

	/**
	 * Registers a new <code>TypeConverter</code> class, which is used to
	 * convert from the strings found in CSS files to the destination type.
	 * Unlike {@link #registerTypeConverter}, which registers a specific
	 * <code>TypeConverter</code> instance, this call registers a class which is
	 * then instantiated for each specific subclass of <code>type</code> that
	 * needs conversion. The <code>converter</code> class must have a
	 * constructor which takes a <code>Class</code>.
	 * <p>
	 * This call requires the <code>setGlobalStylesheet</code> AWTPermission.
	 * 
	 * @param type
	 *            the class to register
	 * @param converter
	 *            the <code>TypeConverter</code> class which provides conversion
	 * @throws SecurityException
	 *             if the required permission is not available
	 */
	public static void registerTypeConverterClass(Class type, Class<? extends TypeConverter> converter) {
		checkPermission();
		converters.put(type, converter);
	}

	/**
	 * Returns the <code>TypeConverter</code> to use when converting strings
	 * into the specified type. Additional <code>TypeConverters</code> are
	 * registered with the {@link #registerTypeConverter} method.
	 * 
	 * @param type
	 *            the class into which to convert the string
	 * @return the TypeConverter to use for the conversion, or <code>null</code>
	 *         if none is registered
	 * @see #registerTypeConverter
	 */
	public static TypeConverter getTypeConverter(Class type) {
		Object result = cachedConverters.get(type);
		if (result == null) {
			result = converters.get(type);
			if (result instanceof Class) {
				try {
					Constructor c = ((Class) result).getConstructor(Class.class);
					result = c.newInstance(type);
					cachedConverters.put(type, (TypeConverter) result);
				} catch (Exception e) {
					throw new StylesheetException(e);
				}
			}
		}
		return (TypeConverter) result;
	}

	/**
	 * Converts a <code>String</code> into the specified type. The
	 * <code>TypeConverter</code> registered to the required class is used to
	 * perform the conversion. Primitive types will be wrapped; for example if
	 * you request <code>int.class</code> the return type will actually be an
	 * <code>Integer</code>.
	 * 
	 * @param string
	 *            the string to convert
	 * @param type
	 *            the type into which to convert the string
	 * @return the converted object
	 * @see #getTypeConverter
	 * @see #registerTypeConverter
	 */
	public static Object convertFromString(String string, Class type) {
		TypeConverter converter = getTypeConverter(type);
		if (converter == null)
			throw new IllegalArgumentException("unsupported type: " + type);
		return converter.convertFromString(string);
	}

	/**
	 * Registers a new <code>Interpolator</code>, which is used to interpolate
	 * values during animated transitions. The <code>Interpolator</code> will be
	 * called whenever the a property of the specified class or any of its
	 * descendents (which do not have a more specific <code>Interpolator</code>
	 * registered) is being animated.
	 * <p>
	 * This call requires the <code>setGlobalStylesheet</code> AWTPermission.
	 * 
	 * @param type
	 *            the class to register
	 * @param interpolator
	 *            the <code>Interpolator</code> which provides support for the
	 *            class
	 * @throws SecurityException
	 *             if the required permission is not available
	 */
	public static void registerInterpolator(Class type, Interpolator interpolator) {
		checkPermission();
		interpolators.put(type, interpolator);
	}

	/**
	 * Returns the <code>Interpolator</code> to use when interpolating values of
	 * the specified type. Additional <code>Interpolators</code> are registered
	 * with the {@link #registerInterpolator} method.
	 * 
	 * @param type
	 *            the type of value being interpolated
	 * @return the Interpolator to use for the interpolation, or
	 *         <code>null</code> if none is registered
	 * @see #registerInterpolator
	 */
	public static Interpolator getInterpolator(Class type) {
		return interpolators.get(type);
	}

	public static Object interpolate(Styleable object, Object start, Object end, float fraction) {
		Interpolator interpolator = getInterpolator(start.getClass());
		if (interpolator == null)
			throw new StylesheetException("there is no interpolator " + "registered for class " + start.getClass());
		if (fraction == 0)
			return start;
		else if (fraction == 1)
			return end;
		else
			return interpolator.interpolate(object, start, end, fraction);
	}

	/**
	 * Splits a comma-separated string into individual arguments. Commas
	 * appearing inside of parentheses are ignored, so the string "1, 2, foo(3,
	 * 4)" would be split into ["1", "2", "foo(3, 4)"].
	 */
	public static String[] parseArgs(String args) {
		if (args.length() == 0)
			return new String[0];
		List<String> result = new ArrayList<String>();
		StringBuilder token = new StringBuilder();
		int depth = 0;
		for (int i = 0; i < args.length(); i++) {
			char c = args.charAt(i);
			switch (c) {
			case '(':
				depth++;
				token.append(c);
				break;
			case ')':
				depth--;
				token.append(c);
				break;
			case ',':
				if (depth == 0) {
					result.add(token.toString().trim());
					token.setLength(0);
					break;
				} // fall through
			default:
				token.append(c);
			}
		}
		result.add(token.toString().trim());
		return result.toArray(new String[result.size()]);
	}

	public static void writeShortUTF(OutputStream out, String string) throws IOException {
		if (string.length() > 255)
			throw new IOException("string '" + string + "' is too long to " + "serialize");
		out.write(string.length());
		byte[] utf = string.getBytes("utf-8");
		out.write(utf);
	}

	public static String readShortUTF(DataInputStream in) throws IOException {
		int length = in.readByte();
		byte[] bytes = new byte[length];
		in.readFully(bytes);
		return new String(bytes, "utf-8");
	}
}