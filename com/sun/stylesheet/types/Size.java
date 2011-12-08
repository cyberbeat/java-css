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

import java.awt.Toolkit;

import com.sun.stylesheet.Styleable;

/**
 * Represents a size specified in a particular unit, such as 14px or 0.2em.
 *
 *@author Ethan Nicholas
 */
public class Size {
    public enum Unit { PERCENT, IN, CM, MM, EM, EX, PT, PC, PX }
    
    private static final int POINTS_PER_INCH = 72;
    private static final float CM_PER_INCH = 2.54f;
    private static final float MM_PER_INCH = CM_PER_INCH * 10;
    private static final int POINTS_PER_PICA = 12;
    
    private float value;
    private Unit unit;

    public Size(float value, Unit unit) {
        // convert all static sizes into points for ease of handling
        switch (unit) {
            case IN: unit = Unit.PT; value *= POINTS_PER_INCH; break;
            case CM: unit = Unit.PT; value *= POINTS_PER_INCH / CM_PER_INCH; 
                     break;
            case MM: unit = Unit.PT; value *= POINTS_PER_INCH / MM_PER_INCH; 
                     break;
            case PC: unit = Unit.PT; value *= POINTS_PER_PICA; break;
            // otherwise we have a relative size and leave it alone
        }
        this.value = value;
        this.unit = unit;
    }
    
    
    private float getFontSize(Styleable object) {
        return ((Size) object.getProperty("font-size")).getSize(object, 
                Unit.PT);
    }
    
    
    private float getEmSize(Styleable object) {
        return getFontSize(object);
    }
    
    
    private float getXHeight(Styleable object) {
        // todo: approximation, need to determine this for real
        return getEmSize(object) / 2; 
    }
    
    
    private static int getDPI() {
        // todo: is it worth determining the actual toolkit in effect for the 
        // styleable, rather than grabbing the default?  Supporting dynamic
        // resolution changes?
        return Toolkit.getDefaultToolkit().getScreenResolution();
    }
    
    
    /**
     * Returns the size converted to the specified unit.  Any unit other than 
     * "percent" may be used.
     *
     *@param object the object against which to compute the size (for percent,
     *              em, and ex sizes)
     *@param unit the output unit
     *@throws NullPointerException if unit is null, or if object is 
     *                             <code>null</code> and the size
     *                             is relative
     */
    public float getSize(Styleable object, Unit unit) {
        if (unit == this.unit)
            return value;
        float points;
        switch (this.unit) {
            case PERCENT: points = value / 100f * getFontSize(object);   break;
            case PT: points = value;                                     break;
            case EM: points = value * getEmSize(object);                 break;
            case EX: points = value * getXHeight(object);                break;
            case PX: points = value * POINTS_PER_INCH / getDPI();        break;
            default: throw new IllegalStateException("Internal error: unit " + 
                    this.unit + " should have been replaced with a point " + 
                    "size by now");
        }
        // we now know the correct size in points, convert it to the output 
        // units
        switch (unit) {
            case PERCENT: throw new IllegalArgumentException("percent is not " +
                    "a valid output unit");
            case IN: return points / POINTS_PER_INCH;
            case CM: return points / POINTS_PER_INCH * CM_PER_INCH;
            case MM: return points / POINTS_PER_INCH * MM_PER_INCH;
            case EM: return points / getEmSize(object);
            case EX: return points / getXHeight(object);
            case PT: return points;
            case PC: return points / POINTS_PER_PICA;
            case PX: return points / POINTS_PER_INCH * getDPI();
            default: throw new Error("Can't-happen error: invalid unit " + 
                    "specified");
        }
    }
    
    
    /** 
     * Converts a relative size into a static size.  Relative sizes are those
     * with percent, em, or ex units, and are relative to the font-size of a
     * particular styleable.  This method returns a new Size with its size
     * specified in points rather than a relative unit, so its size will no
     * longer vary with the input Styleable.
     * <p>
     * If the size is not relative, it returns itself rather than create a new
     * instance.
     *
     *@param object the object against which to resolve sizes
     *@return the computed size (or this size if it is not relative)
     */
    public Size computeSize(Styleable object) {
        if (!isComputed())
            return new Size(getSize(object, Unit.PT), Unit.PT);
        else
            return this;
    }
    
    
    /** 
     * Returns true if the size is a final, ready-to-use value.  Percent, em, 
     * and ex sizes all depend upon the current context and will return
     * <code>false</code>.
     */
    public boolean isComputed() {
        return unit == Unit.PT;
    }
    
    
    public boolean equals(Object o) {
        if (!(o instanceof Size))
            return false;
        
        Size size = (Size) o;
        return size.value == value && size.unit == unit;
    }
    
    
    public int hashCode() {
        return Float.floatToIntBits(value) ^ unit.hashCode();
    }
    
    
    public String toString() {
        return String.valueOf(value) + unit;
    }
}