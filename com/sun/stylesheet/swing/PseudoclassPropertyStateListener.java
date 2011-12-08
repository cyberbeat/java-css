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

import org.jdesktop.beansbinding.PropertyStateEvent;
import org.jdesktop.beansbinding.PropertyStateListener;

import com.sun.stylesheet.PseudoclassEvent;
import com.sun.stylesheet.PseudoclassListener;

// This should really be an inner class of BeanStyleSupport, but as an inner 
// class it was being loaded as part of initializing BeanStyleSupport, whether
// or not it was actually being used.  This created a hard dependency on Beans 
// Binding.  By making it a top-level class, it isn't loaded until specifically 
// required.
class PseudoclassPropertyStateListener implements PropertyStateListener {
    private PseudoclassListener listener;
    private PseudoclassEvent event;

    public PseudoclassPropertyStateListener(PseudoclassListener listener,
            PseudoclassEvent event) {
        this.listener = listener;
        this.event = event;
    }
    
    
    public void propertyStateChanged(PropertyStateEvent e) {
        Object newValue = e.getNewValue();
        if (Boolean.TRUE.equals(newValue))
            listener.pseudoclassAdded(event);
        else if (Boolean.FALSE.equals(newValue))
            listener.pseudoclassRemoved(event);
        else
            throw new IllegalArgumentException("expected TRUE or FALSE, " +
                    "found " + newValue);
    }
}
