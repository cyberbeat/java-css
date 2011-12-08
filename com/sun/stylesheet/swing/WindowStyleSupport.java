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

import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.HashMap;
import java.util.Map;

import com.sun.stylesheet.PseudoclassEvent;
import com.sun.stylesheet.PseudoclassListener;
import com.sun.stylesheet.Styleable;
import com.sun.stylesheet.StylesheetException;
import com.sun.stylesheet.styleable.DefaultStyleable;

/**
 * Implements <code>Styleable</code> support for {@link Window Windows}.
 *
 *@author Ethan Nicholas
 */
public class WindowStyleSupport extends ComponentStyleSupport {
    /** Maps PseudoclassListeners to their WindowListener adapters. */
    private Map<PseudoclassListener, WindowListener> windowListeners;
   
   
    /**
     * Creates a new <code>WindowStyleSupport</code> for the specified class.
     *
     *@param cls the specific window class to support
     */
    public WindowStyleSupport(Class cls) {
        super(cls);
    }
    
    
    @Override
    public Styleable getStyleableParent(Object object) {
        return null;
    }
    
    
    private synchronized WindowListener getWindowListener(
            final PseudoclassListener listener, 
            final PseudoclassEvent event, 
            final boolean inverse) {
        if (windowListeners == null)
            windowListeners = 
                    new HashMap<PseudoclassListener, WindowListener>();
        WindowListener result = windowListeners.get(listener);
        if (result == null) {
            result = new WindowAdapter() {
                @Override
                public void windowActivated(WindowEvent e) {
                    if (inverse)
                        listener.pseudoclassRemoved(event);
                    else
                        listener.pseudoclassAdded(event);
                }
    
    
                @Override
                public void windowDeactivated(WindowEvent e) {
                    if (inverse)
                        listener.pseudoclassAdded(event);
                    else
                        listener.pseudoclassRemoved(event);
                }
            };
            windowListeners.put(listener, result);
        }
        return result;
    }


    @Override
    public void addPseudoclassListener(DefaultStyleable styleable, 
            String pseudoclass, 
            final PseudoclassListener listener) throws StylesheetException {
        final PseudoclassEvent event = new PseudoclassEvent(styleable, 
                pseudoclass);
        Window window = (Window) styleable.getBaseObject();
        if (pseudoclass.equals("active") || pseudoclass.equals("inactive")) {
            boolean inverse = pseudoclass.equals("inactive");
            if (window.isActive() != inverse)
                listener.pseudoclassAdded(event);
            window.addWindowListener(getWindowListener(listener, event, 
                    inverse));
        }
        else
            super.addPseudoclassListener(styleable, pseudoclass, listener);
    }
    
    
    @Override
    public void removePseudoclassListener(DefaultStyleable styleable, 
            String pseudoclass, 
            final PseudoclassListener listener) throws StylesheetException {
        Window window = (Window) styleable.getBaseObject();
        if (pseudoclass.equals("active") || pseudoclass.equals("inactive")) {
            boolean inverse = pseudoclass.equals("inactive");
            window.removeWindowListener(getWindowListener(listener, null, 
                    inverse));
        }
        else
            super.removePseudoclassListener(styleable, pseudoclass, listener);
    }
}