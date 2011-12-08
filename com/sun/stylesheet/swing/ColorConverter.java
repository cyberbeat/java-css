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

import java.awt.Color;
import java.lang.reflect.Field;

import com.sun.stylesheet.StylesheetException;
import com.sun.stylesheet.types.AbstractColorConverter;
 
/**
 * Converts strings into Colors.  Supported strings are those defined by CSS3 
 * (e.g. "#00ff00" or "rgb(0%, 100%, 0%)") with two minor additions.  The string 
 * "null" converts to a null reference, and an eight-digit hex format 
 * (e.g. "#00ff007f") is supported with the final two digits controlling alpha.
 *
 *@author Ethan Nicholas
 */
public class ColorConverter extends AbstractColorConverter {
    protected Object createColor(int r, int g, int b, int alpha) {
        try {
            return new Color(r, g, b, alpha);
        }
        catch (IllegalArgumentException e) {
            throw new StylesheetException(e);
        }
    }


    protected Object createColor(float r, float g, float b, float alpha) {
        try {
            return new Color(r, g, b, alpha);
        }
        catch (IllegalArgumentException e) {
            throw new StylesheetException(e);
        }
    }


    protected Object resolveConstantColor(String name) {
        try {
             Field color = Color.class.getField(name.toUpperCase());
             return color.get(null);
         }
         catch (NoSuchFieldException e) {
             return null;
         }
         catch (IllegalAccessException e) {
             throw new RuntimeException(e);
         }
    }
}