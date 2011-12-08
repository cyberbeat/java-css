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

import java.util.EventObject;

/**
 * Fired when a pseudoclass is added to or removed from a styleable object.
 *
 *@author Ethan Nicholas
 */
public class PseudoclassEvent extends EventObject {
    private final String pseudoclass;

    /**
     * Constructs a new <code>PseudoclassEvent</code>.
     *
     *@param source the affected object
     *@param pseudoclass the pseudoclass which was added or removed
     */
    public PseudoclassEvent(Styleable source, String pseudoclass) {
        super(source);
        
        this.pseudoclass = pseudoclass;
    }
    
    
    /**
     * Returns the pseudoclass which was added or removed.
     *
     *@return the pseudoclass which was added or removed
     */
    public String getPseudoclass() {
        return pseudoclass;
    }
}