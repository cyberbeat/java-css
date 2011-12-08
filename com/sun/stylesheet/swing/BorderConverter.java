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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.border.Border;

import com.sun.stylesheet.types.PrimitiveConverter;
import com.sun.stylesheet.types.TypeConverter;
import com.sun.stylesheet.types.TypeManager;

/** 
 * Converts string representations of {@link Border Borders} into actual 
 * <code>Border</code> objects.  The strings used to represent borders are based 
 * on the methods of {@link BorderFactory}.
 * <p>
 * The "long form" of a border is essentially identical to the Java code one 
 * would normally use, e.g. 
 * <code>BorderFactory.createEtchedBorder(EtchedBorder.RAISED)</code> functions 
 * exactly the same as it would in Java code.  The specified 
 * <code>BorderFactory</code> method is looked up using reflection, and then the 
 * arguments are interpreted using {@link TypeManager} according to the method's 
 * parameter types.  <code>EtchedBorder.RAISED</code> is converted to an 
 * <code>int</code> by {@link PrimitiveConverter}, which understands 
 * <code>int</code>-typed constants, and then the <code>BorderFactory</code> 
 * method is called resulting in a <code>Border</code> object.
 * <p>
 * For brevity, the string <code>"BorderFactory.create"</code> may be omitted.  
 * The parentheses may also be omitted if the function takes no parameters.  The 
 * string <code>"null"</code> is also valid.  Thus the following strings are all 
 * legal <code>Borders</code>:
 * <p>
 * null<br>
 * etchedBorder<br>
 * etchedBorder(EtchedBorder.RAISED)<br>
 * BorderFactory.createEtchedBorder(javax.swing.border.EtchedBorder.LOWERED)<br>
 * compoundBorder(lineBorder(blue, 4), lineBorder(red, 4))<br>
 * BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Title")
 *
 *@author Ethan Nicholas
 */
public class BorderConverter implements TypeConverter<Border> {
    private Pattern pattern = Pattern.compile("(?:BorderFactory\\.create)?" + 
            "(\\w+)\\s*(?:\\((.*)\\))?");
    private Method[] methods;

    public Border convertFromString(String string) {
        if (string.equals("null"))
            return null;
        try {
            Matcher m = pattern.matcher(string);
            if (m.matches()) {
                String methodName = m.group(1);
                if (methodName.length() > 1) {
                    methodName = "create" + 
                            Character.toUpperCase(methodName.charAt(0)) + 
                            methodName.substring(1);
                    String[] args;
                    String argList = m.group(2);
                    if (argList != null)
                        args = TypeManager.parseArgs(argList);
                    else
                        args = new String[0];
                    if (methods == null)
                        methods = BorderFactory.class.getMethods();
                    for (int i = 0; i < methods.length; i++) {
                        Class[] parameterTypes = methods[i].getParameterTypes();
                        if (methods[i].getName().equals(methodName) &&
                                parameterTypes.length == args.length) {
                            Object[] convertedArgs = new Object[args.length];
                            for (int j = 0; j < args.length; j++)
                                convertedArgs[j] = 
                                        TypeManager.convertFromString(args[j], 
                                        parameterTypes[j]);
                            return (Border) methods[i].invoke(null, 
                                    convertedArgs);
                        }
                    }
                    throw new IllegalArgumentException("BorderFactory does" +
                            "not have a method named " + methodName + 
                            " which takes " + args.length + " arguments");
                }
            }
            throw new IllegalArgumentException("cannot convert '" + string + 
                    "' to type Border");
        }
        catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}