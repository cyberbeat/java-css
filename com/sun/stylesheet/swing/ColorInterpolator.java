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

import java.awt.Color;

import com.sun.stylesheet.Styleable;
import com.sun.stylesheet.types.Interpolator;

public class ColorInterpolator implements Interpolator<Color> {
    public Color interpolate(Styleable object, Color start, Color end, 
            float fraction) {
        return new Color(
                start.getRed() + (int) ((end.getRed() - start.getRed()) 
                    * fraction),
                start.getGreen() + (int) ((end.getGreen() - start.getGreen()) 
                    * fraction),
                start.getBlue() + (int) ((end.getBlue() - start.getBlue()) 
                    * fraction),
                start.getAlpha() + (int) ((end.getAlpha() - start.getAlpha()) 
                    * fraction));
    }
}