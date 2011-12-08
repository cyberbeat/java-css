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

/* Modified by Volker Härtel, 8 Dec 2011 */ package com.sun.stylesheet.swing;

import java.awt.Component;
import java.awt.Container;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JComponent;

import com.sun.stylesheet.PseudoclassEvent;
import com.sun.stylesheet.PseudoclassListener;
import com.sun.stylesheet.Styleable;
import com.sun.stylesheet.Stylesheet;
import com.sun.stylesheet.StylesheetException;
import com.sun.stylesheet.styleable.DefaultStyleable;
import com.sun.stylesheet.types.TypeManager;

/**
 * Implements <code>Styleable</code> support for {@link Component Components}.
 *
 *@author Ethan Nicholas
 */
public class ComponentStyleSupport extends BeanStyleSupport {
    static Class jsgPanel;
    static Method getScene;
    static Method getFxComponentFor;
    
    static {
        try {
            Class fxComponent = Class.forName("javafx.gui.Component");
            getFxComponentFor = fxComponent.getMethod("getComponentFor", 
                    JComponent.class);
        }
        catch (ClassNotFoundException e) {
            // ignore
        }
        catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        
        try {
            jsgPanel = Class.forName("com.sun.scenario.scenegraph.JSGPanel");
            getScene = jsgPanel.getMethod("getScene");
        }
        catch (ClassNotFoundException e) {
            // ignore
        }
        catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
    
    
    /** Maps PseudoclassListeners to their mouse over adapters. */
    private Map<PseudoclassListener, MouseListener> mouseOverListeners;

    /** Maps PseudoclassListeners to their mouse down adapters. */
    private Map<PseudoclassListener, MouseListener> mouseDownListeners;


    /** Maps PseudoclassListeners to their FocusListener adapters. */
    private Map<PseudoclassListener, FocusListener> focusListeners;
   
   
    /**
     * Creates a new <code>ComponentStyleSupport</code> for the specified class.
     *
     *@param cls the specific component class to support
     */
    public ComponentStyleSupport(Class cls) {
        super(cls);
    }
    

    /**
     * Returns the value of {@link Component#getName}.
     *
     *@param object the object whose ID is being determined
     *@return the object's ID
     */
    public String getID(Object object) {
        return ((Component) object).getName();
    }
 

    /**
     * Returns the component's style class.  {@link Component Components} do not 
     * support style classes and so return <code>null</code>, but {@link 
     * JComponent JComponents} return the value of 
     * <code>getClientProperty(Stylesheet.STYLE_CLASS_KEY)</code>.
     *
     *@param object the object whose style class is being determined
     *@return the object's style clas
     */
    public String getStyleClass(Object object) {
        if (object instanceof JComponent)
            return (String) ((JComponent) object).getClientProperty(
                    Stylesheet.STYLE_CLASS_KEY);
        else
            return null;
    }
    
    
    public Class[] getObjectClasses(Object object) {
        Class[] result = super.getObjectClasses(object);
        if (object instanceof JComponent && getFxComponentFor != null) {
            try {
                Object fxObject = getFxComponentFor.invoke(null, object);
                if (fxObject != null) {
                    Method getRootJComponent = fxObject.getClass().getMethod(
                            "getRootJComponent");
                    if (getRootJComponent.invoke(fxObject) == object) {
                        Class[] tmp = result;
                        Class[] interfaces = 
                                fxObject.getClass().getInterfaces();
                        result = new Class[tmp.length + interfaces.length + 1];
                        System.arraycopy(tmp, 0, result, 0, tmp.length);
                        System.arraycopy(interfaces, 0, result, tmp.length, 
                                interfaces.length);
                        result[result.length - 1] = fxObject.getClass();
                    }
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        return result;
    }
    
    
    /**
     * Returns the component's parent wrapped in a <code>Styleable</code>.
     *
     *@param object the object whose parent is being determined
     *@return the object's parent
     */
    public Styleable getStyleableParent(Object object) {
        Container parent = ((Component) object).getParent();
        return parent != null ? TypeManager.getStyleable(parent) : null;
    }


    /**
     * Returns the component's children wrapped in a <code>Styleable</code>.
     *
     *@param object the object whose children are being determined
     *@return the object's children
     */
    public Styleable[] getStyleableChildren(Object object) {
        if (jsgPanel != null && jsgPanel.isInstance(object)) {
            try {
                return new Styleable[] { 
                        TypeManager.getStyleable(getScene.invoke(object)) };
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        else if (object instanceof Container) {
            Container c = (Container) object;
            Styleable[] result = new Styleable[c.getComponentCount()];
            for (int i = 0; i < result.length; i++)
                result[i] = TypeManager.getStyleable(c.getComponent(i));
            return result;
        }
        else
            return new Styleable[0];
    }

    @Override
    public void addHierarchyListener(final DefaultStyleable stylable) {
        Object o = stylable.getBaseObject();
        if (o instanceof Container) {
            ((Container) o).addContainerListener(new ContainerListener() {
                public void componentAdded(ContainerEvent e) {
                    Styleable object = TypeManager.getStyleable(e.getChild());
                    stylable.childAdded(object);
                }

                public void componentRemoved(ContainerEvent e) {
                    Styleable object = TypeManager.getStyleable(e.getChild());
                    stylable.childRemoved(object);
                }
            });
        }
    }

    private synchronized FocusListener getFocusListener(
            final PseudoclassListener listener, 
            final PseudoclassEvent event, 
            final boolean inverse) {
        if (focusListeners == null)
            focusListeners = new HashMap<PseudoclassListener, FocusListener>();
        FocusListener result = focusListeners.get(listener);
        if (result == null) {
            result = new FocusListener() {
                public void focusGained(FocusEvent e) {
                    if (inverse)
                        listener.pseudoclassRemoved(event);
                    else
                        listener.pseudoclassAdded(event);
                }
    
    
                public void focusLost(FocusEvent e) {
                    if (inverse)
                        listener.pseudoclassAdded(event);
                    else
                        listener.pseudoclassRemoved(event);
                }
            };
            focusListeners.put(listener, result);
        }
        return result;
    }


    private synchronized MouseListener getMouseOverListener(
            final PseudoclassListener listener, 
            final PseudoclassEvent event, 
            final boolean inverse) {
        if (mouseOverListeners == null)
            mouseOverListeners = 
                    new HashMap<PseudoclassListener, MouseListener>();
        MouseListener result = mouseOverListeners.get(listener);
        if (result == null) {
            result = new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    if (inverse)
                        listener.pseudoclassRemoved(event);
                    else
                        listener.pseudoclassAdded(event);
                }
    
    
                @Override
                public void mouseExited(MouseEvent e) {
                    if (inverse)
                        listener.pseudoclassAdded(event);
                    else
                        listener.pseudoclassRemoved(event);
                }
            };
            mouseOverListeners.put(listener, result);
        }
        return result;
    }
    
    
    private synchronized MouseListener getMouseDownListener(
            final PseudoclassListener listener, 
            final PseudoclassEvent event, 
            final boolean inverse) {
        if (mouseDownListeners == null)
            mouseDownListeners = 
                    new HashMap<PseudoclassListener, MouseListener>();
        MouseListener result = mouseDownListeners.get(listener);
        if (result == null) {
            result = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (inverse)
                        listener.pseudoclassRemoved(event);
                    else
                        listener.pseudoclassAdded(event);
                }
    
    
                @Override
                public void mouseReleased(MouseEvent e) {
                    if (inverse)
                        listener.pseudoclassAdded(event);
                    else
                        listener.pseudoclassRemoved(event);
                }
            };
            mouseDownListeners.put(listener, result);
        }
        return result;
    }
    
    
    public void addPseudoclassListener(DefaultStyleable styleable, 
            String pseudoclass, final PseudoclassListener listener) 
            throws StylesheetException {
        final PseudoclassEvent event = new PseudoclassEvent(styleable, 
                pseudoclass);
        Component component = (Component) styleable.getBaseObject();
        if (pseudoclass.equals("focused") || pseudoclass.equals("unfocused")) {
            boolean inverse = pseudoclass.equals("unfocused");
            if (component.hasFocus() != inverse)
                listener.pseudoclassAdded(event);
            component.addFocusListener(getFocusListener(listener, event, 
                    inverse));
        }
        else if (pseudoclass.equals("mouseover") || 
                pseudoclass.equals("mouseout")) {
            boolean inverse = pseudoclass.equals("mouseout");
            if (inverse)
                listener.pseudoclassAdded(event);
            component.addMouseListener(getMouseOverListener(listener, event, 
                    inverse));
        }
        else if (pseudoclass.equals("mousedown") || 
                pseudoclass.equals("mouseup")) {
            boolean inverse = pseudoclass.equals("mouseup");
            if (inverse)
                listener.pseudoclassAdded(event);
            component.addMouseListener(getMouseDownListener(listener, event, 
                    inverse));
        }
        else
            super.addPseudoclassListener(styleable, pseudoclass, listener);
    }
    
    
    public void removePseudoclassListener(DefaultStyleable styleable, 
            String pseudoclass, final PseudoclassListener listener) 
            throws StylesheetException {
        Component component = (Component) styleable.getBaseObject();
        if (pseudoclass.equals("focused") || pseudoclass.equals("unfocused")) {
            boolean inverse = pseudoclass.equals("unfocused");
            component.removeFocusListener(getFocusListener(listener, null, 
                    inverse));
        }
        else if (pseudoclass.equals("mouseover") || 
                pseudoclass.equals("mouseout")) {
            boolean inverse = pseudoclass.equals("mouseout");
            component.removeMouseListener(getMouseOverListener(listener, null, 
                    inverse));
        }
        else if (pseudoclass.equals("mousedown") || 
                pseudoclass.equals("mouseup")) {
            boolean inverse = pseudoclass.equals("mouseup");
            component.removeMouseListener(getMouseDownListener(listener, null, 
                    inverse));
        }
        else
            super.removePseudoclassListener(styleable, pseudoclass, listener);
    }
}
