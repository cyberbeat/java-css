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

import java.beans.PropertyDescriptor;

import com.sun.stylesheet.StylesheetException;
import com.sun.stylesheet.styleable.DefaultPropertyHandler;
import com.sun.stylesheet.styleable.StyleSupport;

/**
 * Provides support for the <code>text</code> property.  While <code>text</code> 
 * would ordinarily work just fine with the default support, the presence of 
 * <code>text-decoration</code> means that <code>text</code> has to be smart 
 * enough to preserve the decorations when it is set.
 *
 *@author Ethan Nicholas
 */
class TextHandler extends DefaultPropertyHandler {
    private TextDecorationHandler textDecorationHandler;
    private StyleSupport styleSupport;

    public TextHandler(StyleSupport styleSupport, 
            PropertyDescriptor descriptor) {
        super(descriptor);
        
        this.styleSupport = styleSupport;
    }
    

    TextDecorationHandler getTextDecorationHandler() {
        if (textDecorationHandler == null)
            textDecorationHandler = (TextDecorationHandler) 
                    styleSupport.getPropertyHandler("text-decoration");
        return textDecorationHandler;
    }
    
    
    // Note that getProperty() currently returns the decorated text, which may 
    // differ from the undecorated text applied by setProperty().  I don't think 
    // this creates any problems, but it's something to be aware of.
    @Override
    public void setProperty(Object object, Object value) throws StylesheetException {
        super.setProperty(object, value);
        getTextDecorationHandler().setProperty(object, 
                getTextDecorationHandler().getProperty(object));
    }
}