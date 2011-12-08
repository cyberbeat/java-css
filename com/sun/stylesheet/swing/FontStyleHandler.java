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

/**
 * Provides support for the font-style synthetic property.
 *
 *@author Ethan Nicholas
 */
class FontStyleHandler extends DefaultPropertyHandler {
    enum FontStyle { PLAIN, ITALIC };

    public FontStyleHandler(PropertyDescriptor descriptor) {
        super(descriptor);
    }
    
    
    @Override
    public Class getPropertyType(Object object) {
        return FontStyle.class;
    }
    
    
    @Override
    public Object getProperty(Object object) throws StylesheetException {
        Font font = (Font) super.getProperty(object);
        if (font == null || (font.getStyle() & Font.ITALIC) == 0)
            return FontStyle.PLAIN;
        else
            return FontStyle.ITALIC;
    }
    
    
    @Override
    public void setProperty(Object object, Object value) throws StylesheetException {
        if (!(value instanceof FontStyle))
            throw new IllegalArgumentException(value + 
                    " is not an instance of FontStyle");
        Font old = (Font) super.getProperty(object);
        if (old != null) {
            int style = old.getStyle();
            if (value == FontStyle.PLAIN)
                style = style & ~Font.ITALIC;
            else if (value == FontStyle.ITALIC)
                style = style | Font.ITALIC;
            else
                throw new IllegalStateException("can't happen");
            // have to be careful to preserve the float size -- if the value
            // changes, PropertyManager will conclude that the value has been 
            // overridden by the developer.  Unfortunately deriveFont(style)
            // unexpectedly rounds the font size to an int.
            float size = old.getSize2D();
            super.setProperty(object, old.deriveFont(style).deriveFont(size));
        }
    }
}