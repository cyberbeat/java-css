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

/* Modified by Volker Härtel, 8 Dec 2011 */ package com.sun.stylesheet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.sun.stylesheet.types.Size;
import com.sun.stylesheet.types.TypeManager;

public class Declaration {
	private String propertyName;
	private String value;
	private boolean important;
	private Animation animation;
	
	public Declaration(String propertyName, String value) {
        this(propertyName, value, false);
    }
    

	public Declaration(String propertyName, String value, boolean important) {
	    this(propertyName, value, important, null);
	}
	

	public Declaration(String propertyName, String value, boolean important,
	        Animation animation) {
		this.propertyName = propertyName;
		this.value = value;
		this.important = important;
		this.animation = animation;
	}
	

	public String getPropertyName() {
		return propertyName;
	}
	
	
	public String getValue() {
		return value;
	}
	
	
	public boolean isImportant() {
	    return important;
    }
    
    
    public Animation getAnimation() {
        return animation;
    }
	
	
	private Object convertValue(Styleable object) {
        Object convertedValue = object.convertPropertyFromString(
                propertyName, value);
        if (convertedValue instanceof Size)
            convertedValue = ((Size) convertedValue).computeSize(object);	
        return convertedValue;
    }
    
    
    public void applyTo(final Styleable object, Rule source, 
	        Priority priority) {
	    Stylesheet stylesheet = source.getStylesheet();
	    if (stylesheet != null && !stylesheet.supportsPriority())
	        object.setProperty(propertyName, convertValue(object));
        else {
            PropertyManager.applyProperty(object, propertyName, 
                    convertValue(object), source, priority, animation);
        }
	}
	
	
	public void removeFrom(final Styleable object, Rule source, 
	        Priority priority) {
        PropertyManager.removeProperty(object, propertyName, 
                convertValue(object), source, priority, animation);
	}
	
	
	void writeBinary(DataOutputStream out) throws IOException {
	    TypeManager.writeShortUTF(out, propertyName);
	    TypeManager.writeShortUTF(out, value);
	    out.writeByte((important ? 2 : 0) + (animation != null ? 1 : 0));
	    if (animation != null)
	        animation.writeBinary(out);
    }


    static Declaration readBinary(DataInputStream in) throws IOException {
        String propertyName = TypeManager.readShortUTF(in);
        String value = TypeManager.readShortUTF(in);
        byte flags = in.readByte();
        boolean important = (flags & 2) != 0;
        Animation animation = null;
        if ((flags & 1) != 0)
            animation = Animation.readBinary(in);
        return new Declaration(propertyName, value, important, animation);
    }


	public String toString() {
	    return propertyName + ": " + value + 
	            (isImportant() ? " !important" : "");
    }
}