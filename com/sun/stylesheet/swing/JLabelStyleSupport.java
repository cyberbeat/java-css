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

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;

import javax.swing.JLabel;

/**
 * Implements <code>Styleable</code> support for {@link JLabel JLabels}.  In 
 * addition to the basic support provided by its superclass, 
 * <code>JLabelStyleSupport</code> adds the 
 * <code>text-decoration</code> synthetic property.
 *
 *@author Ethan Nicholas
 */
public class JLabelStyleSupport extends ComponentStyleSupport {
    /**
     * Creates a new <code>JLabelStyleSupport</code> for the specified class.
     *
     *@param cls the specific subclass of JLabel to support
     */
    public JLabelStyleSupport(Class cls) {
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
}