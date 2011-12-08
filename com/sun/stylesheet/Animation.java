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

package com.sun.stylesheet;

import java.awt.geom.Point2D;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.sun.stylesheet.types.Time;

/**
 * Represents animation properties which control how to vary a value over time.
 *
 *@author Ethan Nicholas
 */
public class Animation {
    public enum Interpolation { DEFAULT_CURVE, LINEAR, EASE_IN, EASE_OUT, 
                EASE_IN_OUT };
    private Time duration;
    private Point2D controlPoint1;
    private Point2D controlPoint2;
    
    
    public Animation(Time duration) {
        this(duration, Interpolation.DEFAULT_CURVE);
    }
    
    
    public Animation(Time duration, Interpolation interpolation) {
        this.duration = duration;
        switch (interpolation) {
            case DEFAULT_CURVE: controlPoint1 = new Point2D.Float(0.25f, 0.1f);
                                controlPoint2 = new Point2D.Float(0.25f, 1.0f);
                                break;
            case EASE_IN:       controlPoint1 = new Point2D.Float(0.42f, 0);
                                controlPoint2 = new Point2D.Float(1, 1);
                                break;
            case EASE_OUT:      controlPoint1 = new Point2D.Float(0, 0);
                                controlPoint2 = new Point2D.Float(0.58f, 1);
                                break;
            case EASE_IN_OUT:   controlPoint1 = new Point2D.Float(0.42f, 0);
                                controlPoint2 = new Point2D.Float(0.58f, 1);
                                break;
        }
        
    }
    
    
    public Animation(Time duration, Point2D controlPoint1, 
            Point2D controlPoint2) {
        this.duration = duration;
        this.controlPoint1 = controlPoint1;
        this.controlPoint2 = controlPoint2;
    }
    
    
    Time getDuration() {
        return duration;
    }
    
    
    Point2D getControlPoint1() {
        return controlPoint1;
    }
    
    
    Point2D getControlPoint2() {
        return controlPoint2;
    }
    
    
    void writeBinary(DataOutputStream out) throws IOException {
        duration.writeBinary(out);
        out.writeFloat((float) controlPoint1.getX());
        out.writeFloat((float) controlPoint1.getY());
        out.writeFloat((float) controlPoint2.getX());
        out.writeFloat((float) controlPoint2.getY());
    }
    
    
    static Animation readBinary(DataInputStream in) throws IOException {
        Time duration = Time.readBinary(in);
        float x1 = in.readFloat();
        float y1 = in.readFloat();
        float x2 = in.readFloat();
        float y2 = in.readFloat();
        Animation result = new Animation(duration);
        result.controlPoint1 = new Point2D.Float(x1, y1);
        result.controlPoint2 = new Point2D.Float(x2, y2);
        return result;
    }
}