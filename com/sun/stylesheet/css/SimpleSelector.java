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

/* Modified by Volker Härtel, 8 Dec 2011 */ package com.sun.stylesheet.css;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.sun.stylesheet.Match;
import com.sun.stylesheet.Selector;
import com.sun.stylesheet.Styleable;
import com.sun.stylesheet.types.TypeManager;

/**
 * A simple selector which behaves according to the CSS standard.
 *
 *@author Ethan Nicholas
 */
public class SimpleSelector implements Selector {
    private static final int MAX_CLASS_DEPTH = 10000;
    
    private String javaClassName;
    private String styleClass;
    private String[] pseudoclasses;
    private String id;
    
    /**
     * Constructs a new SimpleSelector.  The <code>styleClass</code>, 
     * <code>pseudoclass</code>, and <code>id</code> parameters are strict 
     * equality matches, but the <code>javaClassName</code> is matched against 
     * the simple (packageless) name of the target object's class as well as all 
     * of its ancestors.  For example the name "JComponent" will successfully 
     * match against the class <code>javax.swing.JButton</code>, because 
     * <code>JComponent</code> is an ancestor of <code>JButton</code>.
     *
     *@param javaClassName the class name to match against
     *@param styleClass the style class name to match against
     *@param pseudoclasses the pseudoclasses to match against
     *@param id the id to match against
     */
    public SimpleSelector(String javaClassName, String styleClass, 
            String[] pseudoclasses, String id) {
        this.javaClassName = javaClassName;
        this.styleClass = styleClass;
        this.pseudoclasses = pseudoclasses;
        this.id = id;
    }
    
    
    /**
     * Returns a {@link Match} if this selector matches the specified object, or 
     * <code>null</code> otherwise.
     *
     *@param node the object to check for a match
     *@return a {@link Match} if the selector matches, or <code>null</code> 
     *      otherwise
     */
    public Match matches(Styleable node) {
        int classMatch = 0;
        if (javaClassName != null) {
            Class[] classes = node.getObjectClasses();
            for (int i = 0; i < classes.length; i++) {
                Class javaClass = classes[i];
                int currentWeight = MAX_CLASS_DEPTH;
                do {
                    String name = javaClass.getName();
                    if (name.equals(javaClassName) || 
                            name.substring(name.lastIndexOf(".") + 
                            1).equals(javaClassName)) {
                        classMatch = Math.max(classMatch, currentWeight);
                        break;
                    }
                    javaClass = javaClass.getSuperclass();
                    currentWeight--;
                }
                while (javaClass != null);
            }
        }
        
        boolean styleClassMatch = (styleClass == null || 
                styleClass.equals(node.getStyleClass()));

        boolean idMatch = (id == null || id.equals(node.getID()));
        if ((javaClassName == null || classMatch != 0) && styleClassMatch && 
                idMatch) {
            Match.Pseudoclass[] p;
            if (pseudoclasses != null) {
                p = new Match.Pseudoclass[pseudoclasses.length];
                for (int i = 0; i < p.length; i++)
                    p[i] = new Match.Pseudoclass(pseudoclasses[i], node);
            }
            else
                p = null;
            return new Match(p, id != null ? 1 : 0, styleClass != null ? 1 : 0,
                    classMatch);
        }
        else
            return null;
    }
    
    
    public void writeBinary(DataOutputStream out) throws IOException {
        byte bits = 0;
        if (javaClassName != null)
            bits |= 8;
        if (styleClass != null)
            bits |= 4;
        if (pseudoclasses != null)
            bits |= 2;
        if (id != null)
            bits |= 1;
        out.writeByte(bits);
        if (javaClassName != null)
    	    TypeManager.writeShortUTF(out, javaClassName);
        if (styleClass != null)
    	    TypeManager.writeShortUTF(out, styleClass);
        if (pseudoclasses != null) {
            out.writeByte(pseudoclasses.length);
            for (String pseudoclass : pseudoclasses)
        	    TypeManager.writeShortUTF(out, pseudoclass);
        }
        if (id != null)
    	    TypeManager.writeShortUTF(out, id);
    }
    
    
    public static SimpleSelector readBinary(DataInputStream in) 
            throws IOException {
        byte flags = in.readByte();
        String javaClassName = null;
        if ((flags & 8) != 0)
            javaClassName = TypeManager.readShortUTF(in);
        String styleClass = null;
        if ((flags & 4) != 0)
            styleClass = TypeManager.readShortUTF(in);
        String[] pseudoclasses = null;
        if ((flags & 2) != 0) {
            int pseudoclassCount = in.readByte();
            pseudoclasses = new String[pseudoclassCount];
            for (int i = 0; i < pseudoclassCount; i++)
                pseudoclasses[i] = TypeManager.readShortUTF(in);
        }
        String id = null;
        if ((flags & 1) != 0)
            id = TypeManager.readShortUTF(in);
        return new SimpleSelector(javaClassName, styleClass, pseudoclasses, id);
    }
    
    
    /** Converts this object to a string. */
    public String toString() {
        StringBuffer result = new StringBuffer();
        if (javaClassName != null)
            result.append(javaClassName);
        if (styleClass != null)
            result.append("." + styleClass);
        if (id != null)
            result.append("#" + id);
        if (pseudoclasses != null) {
            for (int i = 0; i < pseudoclasses.length; i++)
                result.append(":" + pseudoclasses[i]);
        }
        if (result.length() == 0)
            result.append("*");
        return result.toString();
    }
}