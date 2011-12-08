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

import javax.swing.JComponent;

import com.sun.stylesheet.StylesheetException;
import com.sun.stylesheet.styleable.PropertyHandler;
import com.sun.stylesheet.styleable.StyleSupport;

/**
 * Provides support for the <code>text-decoration</code> synthetic property.
 *
 *@author Ethan Nicholas
 */
public class TextDecorationHandler implements PropertyHandler {
    public enum Decoration { none, underline, linethrough };
    
    private static final StringBuilder TEXT_DECORATION_KEY = 
            new StringBuilder("$cssTextDecoration");
    private static final String UNDERLINE_START = "<html><!-- css --><u>";
    private static final String UNDERLINE_END = "</u>";
    private static final String LINETHROUGH_START = 
            "<html><!-- css --><strike>";
    private static final String LINETHROUGH_END = "</strike>";

    private StyleSupport styleSupport;
    private TextHandler textHandler;

    
    public TextDecorationHandler(StyleSupport styleSupport) {
        this.styleSupport = styleSupport;
    }


    public Class getPropertyType(Object object) {
        return Decoration.class;
    }
    
    
    private String stripText(String text) {
        if (text.startsWith(UNDERLINE_START) && text.endsWith(UNDERLINE_END))
            text = text.substring(UNDERLINE_START.length(), text.length() - 
                    UNDERLINE_END.length());
        if (text.startsWith(LINETHROUGH_START) && 
                text.endsWith(LINETHROUGH_END))
            text = text.substring(LINETHROUGH_START.length(), text.length() - 
                    LINETHROUGH_END.length());
        return text;
    }
    
    
    TextHandler getTextHandler() {
        if (textHandler == null)
            textHandler = (TextHandler) styleSupport.getPropertyHandler("text");
        return textHandler;
    }


    public Object getProperty(Object object) throws StylesheetException {
        JComponent c = (JComponent) object;
        Decoration result = (Decoration) 
                c.getClientProperty(TEXT_DECORATION_KEY);
        return result != null ? result : Decoration.none;
    }
    
    
    public void setProperty(Object object, Object value) throws StylesheetException {
        ((JComponent) object).putClientProperty(TEXT_DECORATION_KEY, value);
        String originalText = (String) getTextHandler().getProperty(object);
        if (originalText != null) {
            String newText = stripText(originalText);
            if (value == Decoration.underline)
                newText = UNDERLINE_START + newText + UNDERLINE_END;
            else if (value == Decoration.linethrough)
                newText = LINETHROUGH_START + newText + LINETHROUGH_END;
            if (!newText.equals(originalText)) 
                textHandler.setProperty(object, newText);
        }
    }
}