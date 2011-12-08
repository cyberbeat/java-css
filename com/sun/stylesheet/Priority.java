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


/**
 * Describes the priority of a property value.  When a property has multiple 
 * values with different priorities assigned to it, the highest-priority value 
 * is used.
 *
 *@author Ethan Nicholas
 */
public class Priority implements Comparable<Priority>, Cloneable {
    private boolean important;
    private int pseudoclassCount;
    private int stylesheetPriority;
    private int depth;
    private int idCount;
    private int styleclassCount;
    private int javaClassWeight;
    private int order;

    /**
     * Constructs a new <code>Priority</code>.  Attributes are listed in order
     * from most to least important: that is, the <code>important</code>
     * attribute carries the most weight and the <code>javaClassWeight</code>
     * the least.  Lower <code>depths</code> are higher priority;  for all other
     * values a higher value is higher priority.
     */
    public Priority(boolean important, int pseudoclassCount, 
            int stylesheetPriority, int depth, int idCount, int styleclassCount,
            int javaClassWeight, int order) {
        this.important = important;
        this.pseudoclassCount = pseudoclassCount;
        this.stylesheetPriority = stylesheetPriority;
        this.depth = depth;
        this.idCount = idCount;
        this.styleclassCount = styleclassCount;
        this.javaClassWeight = javaClassWeight;
        this.order = order;
    }
    
    
    public boolean isImportant() {
        return important;
    }
    

    public void setImportant(boolean important) {
        this.important = important;
    }
    
    
    public int getPseudoclassCount() {
        return pseudoclassCount;
    }
    
    
    public void setPseudoclassCount(int pseudoclassCount) {
        this.pseudoclassCount = pseudoclassCount;
    }
    
    
    public int getStylesheetPriority() {
        return stylesheetPriority;
    }
    
    
    public void setStylesheetPriority(int stylesheetPriority) {
        this.stylesheetPriority = stylesheetPriority;
    }
    
    
    public int getDepth() {
        return depth;
    }
    
    
    public void setDepth(int depth) {
        this.depth = depth;
    }
    
    
    public int getIdCount() {
        return idCount;
    }
    
    
    public int getOrder() {
        return order;
    }
    
    
    public void setIdCount(int idCount) {
        this.idCount = idCount;
    }
    
    
    public int getStyleclassCount() {
        return styleclassCount;
    }
    
    
    public void setStyleclassCount(int styleclassCount) {
        this.styleclassCount = styleclassCount;
    }
    
    
    public int getJavaClassWeight() {
        return javaClassWeight;
    }
    
    
    public void setJavaClassWeight(int javaClassWeight) {
        this.javaClassWeight = javaClassWeight;
    }
    
    
    public void setOrder(int order) {
        this.order = order;
    }
    
    
    /**
     * Returns <code>true</code> if this object is equal to the specified 
     * object.
     */
    public boolean equals(Object o) {
        if (!(o instanceof Priority))
            return false;
        
        Priority p = (Priority) o;
        return important == p.important &&
                pseudoclassCount == p.pseudoclassCount &&
                stylesheetPriority == p.stylesheetPriority &&
                depth == p.depth &&
                idCount == p.idCount &&
                styleclassCount == p.styleclassCount &&
                javaClassWeight == p.javaClassWeight &&
                order == p.order;
    }
    
    
    /**
     * Returns the hash code for this object.
     */
    public int hashCode() {
        return important ? 1 : 0 ^
                pseudoclassCount  ^
                stylesheetPriority ^
                depth ^
                idCount ^
                styleclassCount ^
                javaClassWeight ^
                order;
    }
    
    
    /**
     * Compares two priorities.  
     *
     *@param p the priority to compare to
     *@return a negative number if this priority is lower, zero if they are 
     *      equal, and a positive number if this priority is higher
     */
    public int compareTo(Priority p) {
        int result = (important ? 1 : 0) - (p.important ? 1 : 0);
        if (result != 0)
            return result;
        result = pseudoclassCount - p.pseudoclassCount;
        if (result != 0)
            return result;
        result = stylesheetPriority - p.stylesheetPriority;
        if (result != 0)
            return result;
        result = p.depth - depth; // note that depth order is reversed
        if (result != 0)
            return result;
        result = idCount - p.idCount;
        if (result != 0)
            return result;
        result = styleclassCount - p.styleclassCount;
        if (result != 0)
            return result;
        result = javaClassWeight - p.javaClassWeight;
        if (result != 0)
            return result;
        result = order - p.order;
        return result;
    }
    
    
    public String toString() {
        return "Priority[" + important + ", " + pseudoclassCount + ", " +
                stylesheetPriority + ", " + depth + ", " + idCount + ", " +
                styleclassCount + ", " + javaClassWeight + "," + order + "]";
    }
    
    
    public Object clone() {
        try {
            return super.clone();
        }
        catch (CloneNotSupportedException e) {
            throw new Error("can't happen");
        }
    }
}

