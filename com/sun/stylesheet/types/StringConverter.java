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

import com.sun.stylesheet.StylesheetException;

/**
 * Converts quoted strings which may contain escape sequences by removing the 
 * quotes and expanding the escape sequences.  Input strings must be surrounded
 * by either single (') or double (") quotes and may contain valid Java string
 * escape sequences such as \n.
 */
public class StringConverter implements TypeConverter<String> {
    public String convertFromString(String string) {
        String trimmed = string.trim();
        if (trimmed.equals("null"))
            return null;
        if ((trimmed.startsWith("\"") && trimmed.endsWith("\"")) ||
                (trimmed.startsWith("'") && trimmed.endsWith("'"))) {
            trimmed = trimmed.substring(1, trimmed.length() - 1);
            StringBuilder result = new StringBuilder(trimmed.length());
            for (int i = 0; i < trimmed.length(); i++) {
                char c = trimmed.charAt(i);
                if (c == '\\') {
                    try {
                        char escape = trimmed.charAt(i + 1); 
                        switch (escape) {
                            case '"': result.append('"'); break;
                            case '\'': result.append('\''); break;
                            case '\\': result.append('\\'); break;
                            case 'n': result.append('\n'); break;
                            case 'r': result.append('\r'); break;
                            case 't': result.append('\t'); break;
                            case 'b': result.append('\b'); break;
                            default: throw new StylesheetException("invalid " + 
                                    "string escape \\" + escape);
                        }
                    }
                    catch (StringIndexOutOfBoundsException e) {
                        throw new StylesheetException("invalid string: " + 
                                string);
                    }
                    i++;
                }
                else
                    result.append(c);
            }
            return result.toString();
        }
        else
            throw new StylesheetException("expected quoted string, found '" + string + 
                    "'");
    }
}