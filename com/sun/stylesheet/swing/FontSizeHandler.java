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

import java.awt.Font;
import java.beans.PropertyDescriptor;

import com.sun.stylesheet.StylesheetException;
import com.sun.stylesheet.styleable.DefaultPropertyHandler;
import com.sun.stylesheet.types.Size;
import com.sun.stylesheet.types.TypeManager;

/**
 * Provides support for the font-size synthetic property.
 *
 *@author Ethan Nicholas
 */
class FontSizeHandler extends DefaultPropertyHandler {
    public FontSizeHandler(PropertyDescriptor descriptor) {
        super(descriptor);
    }
    
    
    @Override
    public Class getPropertyType(Object object) {
        return Size.class;
    }
    
    
    @Override
    public Object getProperty(Object object) throws StylesheetException {
        Font font = (Font) super.getProperty(object);
        return new Size(font != null ? font.getSize2D() : 12, Size.Unit.PT);
    }
    
    
    @Override
    public void setProperty(Object object, Object value) throws StylesheetException {
        Font old = (Font) super.getProperty(object);
        if (old != null) {
            // have to be careful to preserve the float size -- if the value 
            // changes, PropertyManager will conclude that the value has been 
            // overridden by the developer. Unfortunately deriveFont(style) 
            // unexpectedly rounds the font size to an int.
            float size = ((Size) value).getSize(
                    TypeManager.getStyleable(object), 
                    Size.Unit.PT);
            super.setProperty(object, old.deriveFont(size));
        }
    }
}