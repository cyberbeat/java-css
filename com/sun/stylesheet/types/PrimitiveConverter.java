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

package com.sun.stylesheet.types;

import java.lang.reflect.Field;

import com.sun.stylesheet.StylesheetException;

/**
 * Converts string representations of the various "simple" Java types, such as 
 * <code>int</code>, <code>float</code>, and <code>boolean</code>, into their 
 * correct types.
 * <p>
 * <code>short</code>, <code>long</code>, <code>float</code>, and
 * <code>double</code> are simply parsed using their respective parse methods, 
 * such as <code>Float.parseFloat()</code>.
 * <p>
 * <code>int</code> supports all strings recognized by
 * <code>Integer.parseInt()</code>, and
 * additionally supports references to constant fields such as 
 * <code>Font.BOLD</code>.  Classes in <code>java.awt</code>, 
 * <code>javax.swing</code>, and <code>javax.swing.border</code> can be
 * referenced by their simple, unqualified names, while classes in other 
 * packages must be fully named.
 * <p>
 * <code>char</code> requires the input string to be exactly one character long, 
 * and will throw an exception otherwise.
 * <p>
 * <code>boolean</code> requires the input string to be either "true" or "false" 
 * (case insensitive).
 *
 *@author Ethan Nicholas
 */
public class PrimitiveConverter implements TypeConverter<Object> {
    private Class type;
    
    
    public PrimitiveConverter(Class type) {
        this.type = type;
    }
    
    
    public Object convertFromString(String string) {
        try {
            if (type == int.class || type == Integer.class) {
                try {
                    return Integer.valueOf(string);
                }
                catch (NumberFormatException e) {
                    return parseConstant(string);
                }
            }
            else if (type == boolean.class || type == Boolean.class) {
                if (string.toLowerCase().equals("true"))
                    return Boolean.TRUE;
                else if (string.toLowerCase().equals("false"))
                    return Boolean.FALSE;
                else
                    throw new StylesheetException("expected 'true' or 'false'" +
                            ", found '" + string + "'");
            }
            else if (type == byte.class || type == Byte.class)
                return Byte.valueOf(string);
            else if (type == short.class || type == Short.class)
                return Short.valueOf(string);
            else if (type == long.class || type == Long.class)
                return Long.valueOf(string);
            else if (type == float.class || type == Float.class)
                return Float.valueOf(string);
            else if (type == double.class || type == Double.class)
                return Double.valueOf(string);
            else if (type == char.class || type == Character.class) {
                if (string.length() == 1)
                    return new Character(string.charAt(0));
                else
                    throw new StylesheetException("expected a single " +
                            "character, found '" + string + "'");
            }
            else
                throw new IllegalArgumentException("unsupported type: " + type);
        }
        catch (NumberFormatException e) {
            throw new StylesheetException(e);
        }
    }
    
    
    private Class resolveClass(String className) throws ClassNotFoundException {
        String[] imports = { "", "java.awt.", "javax.swing.", 
                "javax.swing.border." };
        for (String s : imports) {
            try {
                return Class.forName(s + className);
            }
            catch (ClassNotFoundException e) {
                // ignore
            }
        }
        throw new ClassNotFoundException(className);
    }
    
    
    private int parseConstant(String string) {
        int dot = string.lastIndexOf(".");
        if (dot != -1) {
            try {
                Class cls = resolveClass(string.substring(0, dot));
                Field f = cls.getField(string.substring(dot + 1));
                return f.getInt(null);
            }
            catch (Exception e) {
                // fall through
            }
        }
        throw new StylesheetException("could not convert string \"" + string + 
                "\" to int");
    }
}