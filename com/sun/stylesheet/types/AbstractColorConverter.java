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

import java.util.Arrays;

import com.sun.stylesheet.StylesheetException;
 
/**
 * Abstract base class for converting strings into colors.  Because this CSS
 * engine must support two completely different color classes (AWT colors and
 * JavaFX colors), this base class handles the basic conversion of color strings
 * into red, green, blue, and alpha components without dictating any particular
 * final output class.
 * <p>
 * Supported strings are those defined by CSS3 (e.g. "#00ff00" or "rgb(0%, 100%, 
 * 0%)" with two minor additions.  The string "null" converts to a null 
 * reference, and an eight-digit hex format (e.g. "#00ff007f") is supported with
 * the final two digits controlling alpha.
 * <p>
 * Concrete subclasses are responsible for taking the computed color channel
 * values and storing them in the final output object, such as 
 * <code>java.awt.Color</code.
 *
 *@author Ethan Nicholas
 */
public abstract class AbstractColorConverter implements TypeConverter {
    public Object convertFromString(String string) {
        if (string.equals("null"))
            return null;
        else if (string.length() == 7 && string.charAt(0) == '#') {
            int r = Integer.parseInt(string.substring(1, 3), 16);
            int g = Integer.parseInt(string.substring(3, 5), 16);
            int b = Integer.parseInt(string.substring(5), 16);
            return createColor(r, g, b);
        }
        else if (string.length() == 9 && string.charAt(0) == '#') {
            int r = Integer.parseInt(string.substring(1, 3), 16);
            int g = Integer.parseInt(string.substring(3, 5), 16);
            int b = Integer.parseInt(string.substring(5, 7), 16);
            int a = Integer.parseInt(string.substring(7), 16);
            return createColor(r, g, b, a);
        }
        else if (string.equals("transparent"))
            return convertFromString("#00000000");
        else if (string.startsWith("rgb(") && string.endsWith(")")) {
            String[] args = TypeManager.parseArgs(
                string.substring("rgb(".length(), string.length() - 1));
            if (args.length != 3)
                throw new StylesheetException("rgb() takes 3 arguments, " +
                        "found " + Arrays.asList(args));
            float r = parseComponent(args[0]);
            float g = parseComponent(args[1]);
            float b = parseComponent(args[2]);
            return createColor(r, g, b);
        }
        else if (string.startsWith("rgba(") && string.endsWith(")")) {
            String[] args = TypeManager.parseArgs(
                string.substring("rgba(".length(), string.length() - 1));
            if (args.length != 4)
                throw new StylesheetException("rgba() takes 4 arguments, " +
                        "found " + Arrays.asList(args));
            float r = parseComponent(args[0]);
            float g = parseComponent(args[1]);
            float b = parseComponent(args[2]);
            float a = parseComponent(args[3], 1);
            return createColor(r, g, b, a);
        }
        else if (string.startsWith("hsl(") && string.endsWith(")")) {
            String[] args = TypeManager.parseArgs(
                string.substring("hsl(".length(), string.length() - 1));
            if (args.length != 3)
                throw new StylesheetException("hsl() takes 3 arguments, " +
                        "found " + Arrays.asList(args));
            float h = Float.parseFloat(args[0]);
            h = (((h % 360) + 360) % 360) / 360;
            float s = parseComponent(args[1], 1);
            float l = parseComponent(args[2], 1);
            float[] rgb = HSLToRGB(h, s, l);
            return createColor(rgb[0], rgb[1], rgb[2], 1);
        }
        else if (string.startsWith("hsla(") && string.endsWith(")")) {
            String[] args = TypeManager.parseArgs(
                string.substring("hsla(".length(), string.length() - 1));
            if (args.length != 4)
                throw new StylesheetException("hsla() takes 4 arguments, " +
                        "found " + Arrays.asList(args));
            float h = Float.parseFloat(args[0]);
            h = (((h % 360) + 360) % 360) / 360;
            float s = parseComponent(args[1], 1);
            float l = parseComponent(args[2], 1);
            float a = parseComponent(args[3], 1);
            float[] rgb = HSLToRGB(h, s, l);
            return createColor(rgb[0], rgb[1], rgb[2], a);
        }
        else {
            Object result = resolveConstantColor(string);
            if (result == null)
                throw new StylesheetException("unable to convert string '" +
                        string + "' to a color");
            return result;
        }
    }
    
    
    protected Object createColor(int r, int g, int b) {
        return createColor(r, g, b, 255);
    }
    
    
    protected abstract Object createColor(int r, int g, int b, int a);


    protected Object createColor(float r, float g, float b) {
        return createColor(r, g, b, 1);
    }
    
    
    protected abstract Object createColor(float r, float g, float b, float a);
    
    
    protected Object resolveConstantColor(String name) {
        return null;
    }


    // translated from ABC example code in the CSS3 spec.  I haven't attempted 
    // to actually understand the algorithm.
    public float[] HSLToRGB(float hue, float saturation, float lightness) {
        float m2;
        if (lightness < 0.5)
            m2 = lightness * (saturation + lightness);
        else
            m2 = lightness + saturation - (lightness * saturation);
        float m1 = lightness * 2 - m2;
        return new float[] { hueToRGB(m1, m2, hue + 1/3f),
                hueToRGB(m1, m2, hue),
                hueToRGB(m1, m2, hue - 1/3f) };
    }
    
    
    // translated from ABC example code in the CSS3 spec.  I haven't attempted 
    // to actually understand the algorithm.
    private float hueToRGB(float m1, float m2, float hue) {
        if (hue < 0)
            hue += 1;
        else if (hue > 1)
            hue -= 1;
        if (hue * 6 < 1)
            return m1 + (m2 - m1) * hue * 6;
        if (hue * 2 < 1)
            return m2;
        if (hue * 3 < 2)
            return m1 + (m2 - m1) * (2/3f - hue) * 6;
        return m1;
    }
    
    
    private float parseComponent(String component) {
        return parseComponent(component, 255);
    }
    
    
    private float parseComponent(String component, float range) {
        float result;
        if (component.endsWith("%"))
            result = Float.parseFloat(component.substring(0, component.length() 
                - 1)) / 100f;
        else 
            result = Float.parseFloat(component) / range;
        if (result < 0 || result > 1)
            throw new StylesheetException("color component out of range: " +
                    component);
        return result;
    }
}