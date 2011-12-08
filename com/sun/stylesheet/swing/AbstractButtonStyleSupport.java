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

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.util.HashMap;
import java.util.Map;

import javax.swing.AbstractButton;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.sun.stylesheet.PseudoclassEvent;
import com.sun.stylesheet.PseudoclassListener;
import com.sun.stylesheet.StylesheetException;
import com.sun.stylesheet.styleable.DefaultStyleable;

/**
 * Implements <code>Styleable</code> support for {@link AbstractButton 
 * AbstractButtons}.  In addition to the basic support provided by its 
 * superclass, <code>AbstractButtonStyleSupport</code> adds support for the 
 * <code>text-decoration</code> synthetic property.
 *
 *@author Ethan Nicholas
 */
public class AbstractButtonStyleSupport extends ComponentStyleSupport {
    private Map<PseudoclassListener, ChangeListener> armedListeners;
   
   
    /**
     * Creates a new <code>AbstractButtonStyleSupport</code> for the specified 
     * class.
     *
     *@param cls the specific button class to support
     */
    public AbstractButtonStyleSupport(Class cls) {
        super(cls);
    }
    
    
    protected void init() throws IntrospectionException {
        super.init();
        properties.put("text-decoration", new TextDecorationHandler(this));
    }
    
    
    protected void createPropertyHandler(PropertyDescriptor descriptor) {
        String name = descriptor.getName();
        if (name.equals("text"))
            properties.put(name, new TextHandler(this, descriptor));
        else
            super.createPropertyHandler(descriptor);
    }
    
    
    private synchronized ChangeListener getArmedListener(
            final PseudoclassListener listener, 
            final PseudoclassEvent event, 
            final boolean inverse) {
        if (armedListeners == null)
            armedListeners = new HashMap<PseudoclassListener, ChangeListener>();
        ChangeListener result = armedListeners.get(listener);
        if (result == null) {
            result = new ChangeListener() {
                public void stateChanged(ChangeEvent e) {
                    AbstractButton button = (AbstractButton) e.getSource();
                    if (inverse != button.getModel().isArmed())
                        listener.pseudoclassAdded(event);
                    else
                        listener.pseudoclassRemoved(event);
                }
            };
            armedListeners.put(listener, result);
        }
        return result;
    }


    public void addPseudoclassListener(DefaultStyleable styleable, 
            String pseudoclass, final PseudoclassListener listener) 
            throws StylesheetException {
        final PseudoclassEvent event = new PseudoclassEvent(styleable, 
                pseudoclass);
        if (pseudoclass.equals("armed") || pseudoclass.equals("unarmed")) {
            boolean inverse = pseudoclass.equals("unarmed");
            AbstractButton button = (AbstractButton) styleable.getBaseObject();
            if (button.getModel().isArmed() != inverse)
                listener.pseudoclassAdded(event);
            button.addChangeListener(getArmedListener(listener, event, 
                    inverse));
        }
        else
            super.addPseudoclassListener(styleable, pseudoclass, listener);
    }
    
    
    public void removePseudoclassListener(DefaultStyleable styleable, 
            String pseudoclass, final PseudoclassListener listener) 
            throws StylesheetException {
        if (pseudoclass.equals("armed") || pseudoclass.equals("unarmed")) {
            boolean inverse = pseudoclass.equals("unarmed");
            AbstractButton button = (AbstractButton) styleable.getBaseObject();
            button.removeChangeListener(getArmedListener(listener, null, 
                    inverse));
        }
        else
            super.removePseudoclassListener(styleable, pseudoclass, listener);
    }
}