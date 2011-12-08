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
 
import java.awt.Insets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sun.stylesheet.types.TypeConverter;
 
public class InsetsConverter implements TypeConverter<Insets> {
    private Pattern pattern = Pattern.compile("(new\\s+Insets\\s*\\(\\s*)?" +
            "(\\d+)\\s*,\\s*(\\d+)\\s*,\\s*(\\d+)\\s*,\\s*(\\d+)\\s*\\)?");


    public Insets convertFromString(String string) {
        if (string.equals("null"))
            return null;
        Matcher m = pattern.matcher(string);
        if (m.matches()) {
            return new Insets(Integer.parseInt(m.group(2)),
                              Integer.parseInt(m.group(3)),
                              Integer.parseInt(m.group(4)),
                              Integer.parseInt(m.group(5)));
        }
        else
            throw new IllegalArgumentException("unable to convert string '" +   
                    string + "' to Insets");
    }
}