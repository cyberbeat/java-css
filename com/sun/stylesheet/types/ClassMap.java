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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/** 
 * A Map implementation which uses Classes as keys.  <code>ClassMap</code> 
 * differs from typical maps in that it takes subclasses into account;  mapping 
 * a class to a value also maps all subclasses of that class to the value.
 * <p>
 * A <code>get</code> operation will return the value associated with the class
 * itself, or failing that, with its nearest ancestor for which there exists a 
 * mapping.  If no mapping exists for the object or any of its superclasses, its
 * interfaces will checked next in declaration order.  The first mapping found 
 * will be returned.
 *
 *@author Ethan Nicholas
 */
class ClassMap<T> extends HashMap<Class, T> {
    /** 
     * Keeps track of automatically-added Classes so we can distinguish them 
     * from user-added Classes.  Unknown Classes are automatically added to the 
     * map during <code>get</code> calls to speed up subsequent requests, but 
     * they must be updated when the mappings for their superclasses are 
     * modified.
     */
    private Set<Class> autoKeys = new HashSet<Class>();


    /** 
     * Returns the value associated with the key <code>Class</code>.  If the
     * class itself does not have a mapping, its superclass will be checked, and 
     * so on until an ancestor class with a mapping is located.  If none of the 
     * class' ancestors have a mapping, <code>null</code> is returned.
     *
     *@param key the class to check
     *@return the mapping for the class
     */
    public T get(Class key) {
        T result = null;
        Class cls = key;
        while (cls != null) {
            result = super.get(cls);
            if (result != null)
                break;
            cls = cls.getSuperclass();
        }

        if (result == null && key.isInterface()) {
            cls = Object.class;
            result = super.get(cls);
        }
        
        if (result == null) {
            Class[] interfaces = key.getInterfaces();
            for (int i = 0; i < interfaces.length; i++) {
                cls = interfaces[i];
                result = get(cls);
                if (result != null)
                    break;
            }
        }            
        
        if (cls != key && result != null) { 
            // no mapping for the class itself, but found one for a superclass
            // or interface
            put(key, result);
            autoKeys.add(key);
        }
        return result;
    }
    
    
    /** Associates a value with a class and all of its descendents.
      *
      *@param key the class to map
      *@param value the value to map to the class
      *@return the old value associated with the class
      */
    public T put(Class key, T value) {
        // remove automatic keys which descend from the class being modified
        T result = remove(key);
        super.put(key, value);
        return result;
    }
    
    
    /**
     * Removes a mapping for a class and all of its descendents.
     *
     *@param key the class to remove
     *@return the old value associated with the class
     */
    public T remove(Class key) {
        T result = super.remove(key);
        if (autoKeys.size() > 0) { 
            // remove all automatic keys which descend from the class being 
            // removed
            Class cls = key;
            Iterator<Class> i = autoKeys.iterator();
            while (i.hasNext()) {
                Class auto = i.next();
                if (cls.isAssignableFrom(auto)) {
                    i.remove();
                    remove(auto);
                
                }
            }
        }
        return result;
    }
}