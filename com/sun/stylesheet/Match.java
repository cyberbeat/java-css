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

import org.w3c.dom.css.CSSRule;

/**
 * Used by {@link CSSRule} to determine whether or not the rule applies to a 
 * given object.
 *
 *@author Ethan Nicholas
/**
 * Returned by {@link Selector#matches} in the event of a match.
 */
public class Match implements Comparable {
    public static class Pseudoclass {
        private String name;
        private Styleable styleable;
        
        public Pseudoclass(String name, Styleable styleable) {
            this.name = name;
            this.styleable = styleable;
        }
        
        
        public String getName() {
            return name;
        }
        
        
        public Styleable getStyleable() {
            return styleable;
        }
    }

    private Pseudoclass[] pseudoclasses;
    private int idCount;
    private int styleClassCount;
    private int javaClassWeight;
    
    
    /**
     * Constructs a new Match.
     *
     *@param pseudoclasses the pseudoclasses which must be in effect for this 
     *      match.  <code>null</code> or an empty array means that the match 
     *      is permanent
     *@param specificity the priority of the match.  Matches with higher 
     *      specificity apply before matches with lower specificity.
     */
    public Match(Pseudoclass[] pseudoclasses, int idCount, int styleClassCount,
            int javaClassWeight) {
        this.pseudoclasses = pseudoclasses;
        this.idCount = idCount;
        this.styleClassCount = styleClassCount;
        this.javaClassWeight = javaClassWeight;
    }
    
    
    /**
     * Returns the pseudoclasses which must be in effect for this match to be 
     * active.  Every pseudoclass in the array must apply simultaneously.  A 
     * <code>null</code> or empty array signifies a permanent match.
     *
     *@return an array of pseudoclasses governing when this match applies
     */
    public Pseudoclass[] getPseudoclasses() {
        return pseudoclasses;
    }
    
    
    /**
     * Returns "ID count" for this selector, for use in priority calculations.
     * For CSS selectors, this is simply the number of IDs in the selector
     * expression.
     */
    public int getIdCount() {
        return idCount;
    }
    
    
    /**
     * Returns "style clas count" for this selector, for use in priority 
     * calculations.  For CSS selectors, this is simply the number of style 
     * classes in the selector expression.
     */
    public int getStyleClassCount() {
        return styleClassCount;
    }
    
    
    /**
     * Returns "Java class weight" for this selector, for use in priority 
     * calculations.  For CSS selectors, this is based on the number of Java
     * classes specified and how closely they match ("JComponent" is a more
     * precise selector than "Object").
     */
    public int getJavaClassWeight() {
        return javaClassWeight;
    }
    

    public boolean equals(Object o) {
        if (!(o instanceof Match))
            return false;
        Match m = (Match) o;
        if (idCount != m.idCount || styleClassCount != m.styleClassCount ||
                javaClassWeight != m.javaClassWeight)
            return false;
        if (pseudoclasses == null)
            return m.pseudoclasses == null;
        else
            return pseudoclasses.equals(m.pseudoclasses);
    }
    
    
    public int hashCode() {
        return (pseudoclasses != null ? pseudoclasses.hashCode() : 0) ^ idCount 
                ^ styleClassCount ^ javaClassWeight;
    }
    
    
    public int compareTo(Object o) {
        Match m = (Match) o;
        int pseudoclasses1 = pseudoclasses != null ? pseudoclasses.length : 0;
        int pseudoclasses2 = m.pseudoclasses != null ? m.pseudoclasses.length : 
                0;
        int result = pseudoclasses1 - pseudoclasses2;
        if (result == 0) {
            result = getIdCount() - m.getIdCount();
            if (result == 0) {
                result = getStyleClassCount() - m.getStyleClassCount();
                if (result == 0)
                    result = getJavaClassWeight() - m.getJavaClassWeight();
            }
        }
        return result;
    }
}
