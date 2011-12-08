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

import java.beans.PropertyDescriptor;

import com.sun.stylesheet.StylesheetException;
import com.sun.stylesheet.UnsupportedPropertyException;

/**
 * Provides get/set support for a JavaBeans property.
 *
 *@author Ethan Nicholas
 */
public class DefaultPropertyHandler implements PropertyHandler {
    protected PropertyDescriptor descriptor;

    public DefaultPropertyHandler(PropertyDescriptor descriptor) {
        this.descriptor = descriptor;
    }
    
    
    public Class getPropertyType(Object object) {
        return descriptor.getPropertyType();
    }
    
    
    public Object getProperty(Object object) throws StylesheetException {
        if (descriptor.getReadMethod() != null) {
            try {
                return descriptor.getReadMethod().invoke(object);
            }
            catch (Exception e) {
                throw new StylesheetException(e);
            }
        }
        else
            throw new UnsupportedPropertyException("property " + 
                    descriptor.getName() + " cannot be read");
    }


    public void setProperty(Object object, Object value) throws StylesheetException {
        if (descriptor.getWriteMethod() != null) {
            try {
                descriptor.getWriteMethod().invoke(object, value);
            }
            catch (Exception e) {
                throw new StylesheetException(e);
            }
        }
        else
            throw new UnsupportedPropertyException("property " +
                    descriptor.getName() + " cannot be written");
    }
}