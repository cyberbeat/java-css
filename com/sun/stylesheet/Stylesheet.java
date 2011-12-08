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

package com.sun.stylesheet;

import java.awt.AWTEvent;
import java.awt.AWTPermission;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.AWTEventListener;
import java.awt.event.WindowEvent;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import com.sun.stylesheet.styleable.DefaultStyleable;
import com.sun.stylesheet.types.TypeManager;

/**
 * A stylesheet which can apply properties to a tree of objects. A stylesheet is
 * a collection of zero or more {@link Rule Rules}, each of which is applied to
 * each object in the tree. Typically the rule will examine the object to
 * determine whether or not it is applicable, and if so it will apply certain
 * property values to the object.
 * <p>
 * Objects to be styled must either implement the {@link Styleable} interface,
 * or have a Styleable wrapper created for them. AWT and Swing Components along
 * with SceneGraph nodes will be wrapped in Styleables automatically.
 * <p>
 * Stylesheets can be parsed from CSS documents or created programmatically.
 * Once created, stylesheets can be freely modified, but the modifications do
 * not affect styled objects until a subsequent {@link #applyTo} or
 * {@link #reapply}.
 * 
 *@author Ethan Nicholas
 */
public class Stylesheet {
	/**
	 * Key used to store an object's style class. JComponents store their style
	 * class via putClientProperty and SGNodes via setAttribute.
	 */
	public static final String STYLE_CLASS_KEY = "styleClass";

	private static Stylesheet globalStylesheet;

	private static AWTEventListener globalEventListener;

	private static DebugWindow debugWindow;

	private Object dummy = new Object();

	/**
	 * Objects to which this stylesheet has been applied. WeakHashMap is used as
	 * a set; the keys are pointed to themselves in order to obtain the weak
	 * reference behavior.
	 */
	public transient Map<Styleable, Object> roots = new WeakHashMap<Styleable, Object>();

	private List<Rule> rules = new ArrayList<Rule>() {
		public boolean add(Rule rule) {
			rule.setStylesheet(Stylesheet.this);
			return super.add(rule);
		}

		public void add(int index, Rule rule) {
			rule.setStylesheet(Stylesheet.this);
			super.add(index, rule);
		}

		public Rule set(int index, Rule rule) {
			rule.setStylesheet(Stylesheet.this);
			return super.set(index, rule);
		}

		public boolean addAll(Collection<? extends Rule> c) {
			for (Rule r : c)
				r.setStylesheet(Stylesheet.this);
			return super.addAll(c);
		}

		public boolean addAll(int index, Collection<? extends Rule> c) {
			for (Rule r : c)
				r.setStylesheet(Stylesheet.this);
			return super.addAll(index, c);
		}
	};

	/** True to automatically re-pack too-small windows. */
	private boolean autopack = true;

	private boolean isStatic = true;

	private boolean supportsPriority = true;

	private int priority;

	/** Creates a new stylesheet which contains no rules. */
	public Stylesheet() {
		if (debugWindow != null)
			debugWindow.addStylesheet(this);
	}

	/**
	 * Creates a stylesheet from an array of rules.
	 * 
	 *@param rules
	 *            the stylesheet's rules
	 */
	public Stylesheet(Rule[] rules) {
		this();
		this.rules.addAll(Arrays.asList(rules));
	}

	/**
	 * Creates a stylesheet from a collection of rules.
	 * 
	 *@param rules
	 *            the stylesheet's rules
	 */
	public Stylesheet(Collection<Rule> rules) {
		this();
		this.rules.addAll(rules);
	}

	/**
	 * Returns the stylesheet's rules. The list may be freely modified, but
	 * modifications will have no effect on styled objects until a subsequent
	 * call to {@link #applyTo or #reapply}.
	 * 
	 *@return the current list of rules
	 *@see #applyTo
	 *@see #reapply
	 */
	public List<Rule> getRules() {
		return rules;
	}

	/**
	 * Returns <code>true</code> if this stylesheet is static. See
	 * {@link #setStatic} for details.
	 * <p>
	 * The default value is <code>false</code>.
	 * 
	 *@return true if this is a static stylesheet
	 *@see #setStatic
	 */
	public boolean isStatic() {
		return isStatic;
	}

	/**
	 * Sets whether this stylesheet is static. Static stylesheets consume much
	 * less memory after application, but cannot be changed or removed once
	 * applied. All priority information is discarded once a static stylesheet
	 * has been fully applied, so that stylesheets applied subsequently will
	 * always override settings from a static stylesheet even if they would not
	 * normally be high enough priority to do so.
	 * <p>
	 * Changes to this setting only effect subsequent applications of the
	 * stylesheet; existing applications are left unchanged. The default value
	 * is <code>false</code>.
	 * 
	 *@param isStatic
	 *            whether or not this stylesheet should be made static
	 */
	public void setStatic(boolean isStatic) {
		this.isStatic = isStatic;
	}

	/**
	 * Returns <code>true</code> if this stylesheet supports priority resolution
	 * between conflicting rules. See {@link #setSupportsPriority} for details.
	 * <p>
	 * The default value is true.
	 * 
	 *@return true if priority support is enabled
	 *@see #setSupportsPriority
	 */
	public boolean supportsPriority() {
		return supportsPriority;
	}

	/**
	 * Enables or disables priority support. Disabling priority support
	 * significantly reduces memory consumption and processing requirements, but
	 * means that in the face of conflicting rules the last one applied "wins"
	 * even if it would not ordinarily be high enough priority to do so.
	 * <p>
	 * Disabling priority also has the side effect of turning off the normal CSS
	 * "cascade" -- <code>JFrame { foreground: blue }</code> will no longer
	 * propagate the value to the JFrame's descendents. You can generally work
	 * around this side effect by changing the rules to apply directly to
	 * descendents, e.g. <code>JFrame * { foreground: blue }</code> will have
	 * the desired effect.
	 * <p>
	 * This is a much more significant optimization than {@link #setStatic()},
	 * causing a dramatic increase in performance at the cost of changed
	 * semantics. This setting is primarily intended for mobile / embedded
	 * devices where both processing and memory are at a premium.
	 * <p>
	 * Changes to this setting only effect subsequent applications of the
	 * stylesheet; existing applications are left unchanged. Disabling priority
	 * support also implies <code>setStatic(true)</code>. The default value is
	 * true.
	 * 
	 *@param supportsPriority
	 *            whether or not priority support should be enabled
	 *@see #supportsPriority
	 */
	public void setSupportsPriority(boolean supportsPriority) {
		this.supportsPriority = supportsPriority;

		if (!supportsPriority)
			setStatic(false);
	}

	/**
	 * Applies this stylesheet to an object tree. The root object must either
	 * implement {@link Styleable} or have a wrapper class registered with
	 * {@link TypeManager}. If the stylesheet is already in effect on the
	 * object, it will be removed and reapplied.
	 * 
	 *@throws IllegalArgumentException
	 *             if root cannot be converted to a Styleable
	 *@throws StylesheetException
	 *             if an error occurs while applying the stylesheet
	 */
	public void applyTo(Object root) throws StylesheetException {
		Styleable styleable = TypeManager.getStyleable(root);
		if (getRoots().containsKey(styleable))
			removeFrom(styleable);
		roots.put(styleable, dummy);
		applyTo(styleable, 0);

		if (styleable instanceof DefaultStyleable) {
			Object object = ((DefaultStyleable) styleable).getBaseObject();
			if (object instanceof Window)
				processWindow((Window) object);
		}

		if (isStatic() && supportsPriority())
			PropertyManager.makeStatic(this, styleable);
	}

	/** INTERNAL USE ONLY. */
	public void applyTo(Styleable node, int depth) throws StylesheetException {
		if (debugWindow != null)
			debugWindow.stylesheetApplied(this, node);
		node.addStylesheet(this, depth);
		PropertyManager.cascadeTo(node, false);
		for (int i = 0; i < rules.size(); i++)
			rules.get(i).applyTo(node, depth, i);

		Styleable[] children = node.getStyleableChildren();
		if (children != null) {
			for (Styleable child : children)
				applyTo(child, depth + 1);
		}
	}

	/**
	 * Returns <code>true</code> if autopacking of windows is enabled. If
	 * autopacking is enabled, {@link #processWindow} may resize windows as they
	 * are styled. The default value is <code>true</code>.
	 * 
	 *@return <code>true</code> if windows are being automatically resized as
	 *         needed
	 *@see #setAutopackingEnabled
	 */
	public boolean isAutopackingEnabled() {
		return autopack;
	}

	/**
	 * Sets the window autopacking flag. If autopacking is enabled,
	 * {@link #processWindow} may resize windows as they are styled. The default
	 * value is <code>true</code>.
	 * 
	 *@param autopack
	 *            <code>true</code> to automatically resize windows as needed
	 *@see #isAutopackingEnabled
	 */
	public void setAutopackingEnabled(boolean autopack) {
		this.autopack = autopack;
	}

	/**
	 * Called after a Window instance has been styled. Windows present a special
	 * challenge, namely that the styles are often applied after the window's
	 * size has been set and often cause the window's minimum size to change,
	 * meaning that they would (without additional measures) be created at
	 * improper sizes. This method provides a hook to deal with this situation.
	 * <p>
	 * By default the behavior is to check {@link #isAutopackingEnabled}. If
	 * autopacking is enabled and the window's current size is smaller than its
	 * minimum size, it is resized to be at least its minimum dimensions. If
	 * autopacking is disabled or the window is at least its minimum size, no
	 * action is taken.
	 * 
	 *@param window
	 *            the window which has just been styled
	 */
	protected void processWindow(Window window) {
		Dimension size = window.getMinimumSize();
		if (window.getWidth() < size.width || window.getHeight() < size.height) {
			size.width = Math.max(window.getWidth(), size.width);
			size.height = Math.max(window.getHeight(), size.height);
			window.setSize(size);
			window.validate();
		}
	}

	/**
	 * Removes this stylesheet from an object tree, removing all styled
	 * properties and event listeners. The root object must either implement
	 * {@link Styleable} or have a wrapper class registered with
	 * {@link TypeManager}. Has no effect if called on a styleable object which
	 * has not had this stylesheet applied to it.
	 * 
	 *@throws IllegalArgumentException
	 *             if root cannot be converted to a Styleable
	 *@throws StylesheetException
	 *             if an error occurs while removing the stylesheet
	 */
	public void removeFrom(Object root) throws StylesheetException {
		Styleable styleable = TypeManager.getStyleable(root);
		if (debugWindow != null)
			debugWindow.stylesheetRemoved(this, styleable);
		PropertyManager.removeStylesheet(this, styleable);
	}

	/**
	 * Removes this stylesheet from all objects to which it has been applied,
	 * removing all styled properties and event listeners.
	 * 
	 *@throws StylesheetException
	 *             if an error occurs while removing the stylesheet
	 */
	public void removeFromAll() throws StylesheetException {
		Set<Styleable> roots = new HashSet<Styleable>(getRoots().keySet());
		for (Styleable r : roots) {
			removeFrom(r);
		}
	}

	/**
	 * Reapplies this stylesheet to all objects which it is currently affecting.
	 * This will cause any changes which have been made to the stylesheet to
	 * take effect.
	 */
	public void reapply() throws StylesheetException {
		Set<Styleable> roots = new HashSet<Styleable>(getRoots().keySet());
		for (Styleable r : roots) {
			applyTo(r);
		}
	}

	// it's a Map simply because there is no WeakHashSet
	private Map<Styleable, Object> getRoots() {
		if (roots == null)
			roots = new WeakHashMap<Styleable, Object>();
		return roots;
	}

	/**
	 * Returns the current global stylesheet. As the stylesheet object could
	 * then be modified and reapplied, this call requires the
	 * "setGlobalStylesheet" {@link AWTPermission}.
	 * 
	 *@return the global stylesheet, or <code>null</code> if none
	 *@throws SecurityException
	 *             if the required permission is not available
	 *@see #setGlobalStylesheet
	 */
	public static Stylesheet getGlobalStylesheet() throws SecurityException {
		SecurityManager security = System.getSecurityManager();
		if (security != null)
			security.checkPermission(new AWTPermission("setGlobalStylesheet"));
		return globalStylesheet;
	}

	/**
	 * Sets this stylesheet as the global stylesheet. The stylesheet will be
	 * automatically applied to all newly opened windows, but existing unstyled
	 * windows are unaffected. If there is already a global stylesheet, it will
	 * be removed and any windows it is currently styling will also be updated
	 * to match the new stylesheet.
	 * <p>
	 * It is generally preferable to manually apply stylesheets to windows
	 * rather than rely on <code>setGlobalStylesheet</code>. As global
	 * stylesheets are applied as the window is being put onscreen, sizing
	 * changes required by the global stylesheet (e.g. font or border properties
	 * cause the window's minimum size to increase), the window's rectangle may
	 * first appear at its "unstyled" size and then immediately snap to its
	 * "styled" size. Applying the stylesheet manually, prior to the initial
	 * <code>pack()</code> or other size computation, avoids this issue. However
	 * global stylesheets are an easy way to intercept dialogs such as those
	 * displayed by {@link javax.swing.JOptionPane} which are not convenient to
	 * manually style.
	 * <p>
	 * This call requires the "listenToAllAWTEvents" and "setGlobalStylesheet"
	 * {@link AWTPermission AWTPermissions}. A <code>null</code> parameter may
	 * be used to remove the global stylesheet.
	 * 
	 *@param stylesheet
	 *            the new global stylesheet. May be <code>null</code>.
	 *@throws SecurityException
	 *             if the required permission is not available
	 *@throws StylesheetException
	 *             if an error occurs applying the stylesheet
	 */
	public static void setGlobalStylesheet(Stylesheet stylesheet) throws SecurityException,
			StylesheetException {
		SecurityManager security = System.getSecurityManager();
		if (security != null)
			security.checkPermission(new AWTPermission("setGlobalStylesheet"));
		if (globalStylesheet != null) {
			Set<Styleable> oldRoots = new HashSet<Styleable>(globalStylesheet.getRoots().keySet());
			globalStylesheet.removeFromAll();
			for (Styleable r : oldRoots)
				stylesheet.applyTo(r);
		}
		globalStylesheet = stylesheet;
		if (globalEventListener == null) {
			globalEventListener = new AWTEventListener() {
				public void eventDispatched(AWTEvent event) {
					if (event.getID() == WindowEvent.WINDOW_OPENED) {
						try {
							Window window = ((WindowEvent) event).getWindow();
							if (window != debugWindow)
								globalStylesheet.applyTo(window);
						} catch (StylesheetException e) {
							throw new RuntimeException(e);
						}
					}
				}
			};
			Toolkit.getDefaultToolkit().addAWTEventListener(globalEventListener,
				AWTEvent.WINDOW_EVENT_MASK);
		}
		;
	}

	/** Returns a string representation of this object. */
	public String toString() {
		StringBuilder result = new StringBuilder();
		for (Rule r : rules) {
			if (result.length() > 0)
				result.append(System.getProperty("line.separator"));
			result.append(r.toString());
		}
		return result.toString();
	}

	public int getPriority() {
		return priority;
	}

	public void setPriority(int priority) {
		if (priority < Byte.MIN_VALUE || priority > Byte.MAX_VALUE)
			throw new IllegalArgumentException("priority must be within the " + "range "
				+ Byte.MIN_VALUE + " to " + Byte.MAX_VALUE);
		this.priority = priority;
	}

	public static Stylesheet readBinary(InputStream in) throws IOException {
		DataInputStream dataIn = new DataInputStream(in);
		byte version = dataIn.readByte();
		if (version != 0)
			throw new IOException("Invalid stylesheet, or stylesheet was "
				+ "written by a newer version of the software");
		byte flags = dataIn.readByte();
		byte priority = dataIn.readByte();
		short ruleCount = dataIn.readShort();
		Rule[] rules = new Rule[ruleCount];
		for (int i = 0; i < ruleCount; i++)
			rules[i] = Rule.readBinary(dataIn);
		Stylesheet result = new Stylesheet(rules);
		result.setAutopackingEnabled((flags & 4) != 0);
		result.setStatic((flags & 2) != 0);
		result.setSupportsPriority((flags & 1) != 0);
		result.setPriority(priority);
		return result;
	}

	public void writeBinary(OutputStream out) throws IOException {
		DataOutputStream dataOut = new DataOutputStream(out);
		dataOut.writeByte(0);
		dataOut.writeByte((autopack ? 4 : 0) | (isStatic ? 2 : 0) | (supportsPriority ? 1 : 0));
		dataOut.writeByte(getPriority());
		dataOut.writeShort(rules.size());
		for (Rule r : rules)
			r.writeBinary(dataOut);
		dataOut.flush();
	}

	public static Stylesheet readCSS(Reader in) throws IOException {
		return com.sun.stylesheet.css.parser.CSSParser.parse(in);
	}

	public void writeCSS(Writer out) throws IOException {
		out.write(toString());
	}

	/**
	 * This API is subject to change and may change or disappear in a future
	 * release.
	 * <p>
	 * Sets the name of this stylesheet for debugging purposes.
	 */
	public void setName(String name) {
		if (debugWindow != null)
			debugWindow.setName(this, name);
	}

	/**
	 * This API is subject to change and may change or disappear in a future
	 * release.
	 * <p>
	 * Displays a debug window which allows stylesheets to be edited and the
	 * styles in effect on individual components to be viewed. This method must
	 * be called before any stylesheets are created.
	 */
	public static void enableDebugging() {
		debugWindow = new DebugWindow();
		debugWindow.setVisible(true);
	}

	/**
	 * This API is subject to change and may change or disappear in a future
	 * release.
	 * <p>
	 * Returns the DebugWindow currently in effect, or null if debugging is not
	 * enabled.
	 */
	public static DebugWindow getDebugWindow() {
		return debugWindow;
	}
}