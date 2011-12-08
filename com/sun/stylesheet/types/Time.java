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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Represents a time specified in a particular unit, such as 500ms or 2s.
 *
 *@author Ethan Nicholas
 */
public class Time {
    public enum Unit { MS, S, M }
    
    private static final int MILLISECONDS_PER_SECOND = 1000;
    private static final int SECONDS_PER_MINUTE = 60;
    
    private float millis;

    public Time(float value, Unit unit) {
        switch (unit) {
            case MS: millis = value; break;
            case S:  millis = value * MILLISECONDS_PER_SECOND; break;
            case M:  millis = value * MILLISECONDS_PER_SECOND *
                            SECONDS_PER_MINUTE; break;
            default: throw new Error("Can't-happen error: invalid unit " + 
                    "specified");
        }
    }
    
    
    public float getTime(Unit unit) {
        switch (unit) {
            case MS: return millis;
            case S: return millis / MILLISECONDS_PER_SECOND;
            case M: return millis / MILLISECONDS_PER_SECOND / 
                            SECONDS_PER_MINUTE;
            default: throw new Error("Can't-happen error: invalid unit " + 
                    "specified");
        }
    }
    
    
    public void writeBinary(DataOutputStream out) throws IOException {
        out.writeFloat(millis);
    }
    
    
    public static Time readBinary(DataInputStream in) throws IOException {
        return new Time(in.readFloat(), Unit.MS);
    }
    
    
    public String toString() {
        return String.valueOf(millis) + "ms";
    }
}