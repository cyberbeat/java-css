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

package com.sun.stylesheet.styleable;

import java.util.Map;

import com.sun.stylesheet.PseudoclassListener;
import com.sun.stylesheet.Styleable;

/**
 * Provides basic <code>Styleable</code> support for all instances of a single 
 * class.
 * <p>
 * This interface does not itself extend <code>Styleable</code>;  it is instead 
 * used by the generic wrapper class {@link DefaultStyleable}.
 *
 *@author Ethan Nicholas
 */
public interface StyleSupport {
    String getID(Object object);
    
    String getStyleClass(Object object);
    
    Class[] getObjectClasses(Object object);
    
    Styleable getStyleableParent(Object object);
    
    Styleable[] getStyleableChildren(Object object);
    
    Class getPropertyType(Object object, String propertyName);
    
    Object getProperty(Object object, String propertyName);
    
    void setProperty(Object object, String propertyName, Object value);
    
    void addPseudoclassListener(DefaultStyleable object, String pseudoclass, 
            PseudoclassListener listener);

    void removePseudoclassListener(DefaultStyleable object, String pseudoclass, 
            PseudoclassListener listener);
    
    Map<String, Object> splitCompoundProperty(Object object, String property, 
                Object value);

    boolean isPropertyInherited(Object object, String propertyName);
    
    PropertyHandler getPropertyHandler(String property);
    
    void addHierarchyListener(DefaultStyleable object);
}