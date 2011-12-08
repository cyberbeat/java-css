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

package com.sun.stylesheet.css;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.sun.stylesheet.Match;
import com.sun.stylesheet.Selector;
import com.sun.stylesheet.Styleable;

/**
 * A compound selector which behaves according to the CSS standard.
 *
 *@author Ethan Nicholas
 */
public class CompoundSelector implements Selector {
    public enum Relationship { child, descendent }
    
    private static final String COMPOUND_PSEUDOCLASS = "$compound";
    
    private Selector[] selectors;
    private Relationship[] relationships;
    
    /**
     * Constructs a new <code>CompoundSelector</code>.  The selector is 
     * composed of one or more <code>Selectors</code>, along with an array of 
     * <code>Relationships</code> indicating the required relationship at each 
     * stage.  There must be exactly one less <code>Relationship</code> than 
     * there are selectors.
     * <p>
     * For example, the paramters <code>[selector1, selector2, selector3]</code> 
     * and <code>[Relationship.child, Relationship.descendent]</code> will match 
     * a component when all of the following conditions hold:
     * <ol>
     * <li>The component itself is matched by selector3
     * <li>The component has an ancestor which is matched by selector2
     * <li>The ancestor matched in step 2 is a direct child of a component 
     * matched by selector1
     * </ol>
     * In other words, the compound selector specified above is (in CSS syntax) 
     * <code>selector1 &gt; selector2 selector3</code>.  The greater-than (&gt;) 
     * between selector1 and selector2 specifies a direct child, whereas the 
     * whitespace between selector2 and selector3 corresponds to
     * <code>Relationship.descendent</code>.
     *
     *@param selectors the selectors that make up this compound selector
     *@param relationships the relationships between the selectors, which are 
     *      interleaved between them
     */
    public CompoundSelector(Selector[] selectors, 
            Relationship[] relationships) {
        this.selectors = selectors;
        this.relationships = relationships;
    }
    
    
    /**
     * Returns a {@link Match} if this selector matches the specified object, or 
     * <code>null</code> otherwise.
     *
     *@param node the object to check for a match
     *@return a {@link Match} if the selector matches, or <code>null</code> 
     *      otherwise
     */
    public Match matches(Styleable node) {
        return matches(node, selectors.length - 1);
    }
    
    
    // checks for a match against a subset of the selectors, from 0 to 
    // lastIndex. for example matches(node, 1) will make sure that the node 
    // matches selector 1, and check for an appropriately-related ancestor 
    // matching selector 0
    private Match matches(Styleable node, int lastIndex) {
        Match initialResult = selectors[lastIndex].matches(node);
        if (initialResult == null || lastIndex == 0)
            return initialResult;
        Styleable ancestor = node.getStyleableParent();
        if (ancestor != null) {
            do {
                Match ancestorMatch = matches(ancestor, lastIndex - 1);
                if (ancestorMatch != null) {
                    Match.Pseudoclass[] pseudoclasses = 
                            initialResult.getPseudoclasses();
                    Match.Pseudoclass[] ancestorPseudoclasses = 
                            ancestorMatch.getPseudoclasses();
                    if (ancestorPseudoclasses != null) {
                        if (pseudoclasses == null)
                            pseudoclasses = ancestorPseudoclasses;
                        else {
                            pseudoclasses = new Match.Pseudoclass[
                                    pseudoclasses.length + 
                                    ancestorPseudoclasses.length];
                            System.arraycopy(ancestorPseudoclasses, 0, 
                                    pseudoclasses, 0, 
                                    ancestorPseudoclasses.length);
                            System.arraycopy(initialResult.getPseudoclasses(), 
                                    0, pseudoclasses,
                                    ancestorPseudoclasses.length, 
                                    initialResult.getPseudoclasses().length);
                        }
                    }        
                    return new Match(pseudoclasses, 
                            initialResult.getIdCount() + 
                                ancestorMatch.getIdCount(),
                            initialResult.getStyleClassCount() +
                                ancestorMatch.getStyleClassCount(),
                            initialResult.getJavaClassWeight() +
                                ancestorMatch.getJavaClassWeight());
                }
                ancestor = ancestor.getStyleableParent();
            }
            while (ancestor != null && relationships[lastIndex - 1] == 
                    Relationship.descendent);
            // Relationship.child will cause this loop to exit after the first 
            // iteration
        }
        return null;
    }
    
    
    public void writeBinary(DataOutputStream out) throws IOException {
        out.writeByte(selectors.length);
        for (Selector selector : selectors) {
            if (!(selector instanceof SimpleSelector))
                throw new IOException("CompoundSelector must consist of only " +
                        "SimpleSelectors to be serialized");
            ((SimpleSelector) selector).writeBinary(out);
        }
        for (Relationship relationship : relationships)
            out.writeByte(relationship.ordinal());
    }
    
    
    public static CompoundSelector readBinary(DataInputStream in) 
            throws IOException {
        int selectorCount = in.readByte();
        Selector[] selectors = new Selector[selectorCount];
        for (int i = 0; i < selectorCount; i++)
            selectors[i] = SimpleSelector.readBinary(in);
        Relationship[] relationships = new Relationship[selectorCount - 1];
        for (int i = 0; i < selectorCount - 1; i++) {
            int value = in.readByte();
            switch (value) {
                case 0: relationships[i] = Relationship.child; break;
                case 1: relationships[i] = Relationship.descendent; break;
                default: throw new IOException("unexpected relationship " +
                        "value: " + value);
            }
        }
        return new CompoundSelector(selectors, relationships);
    }
    
    
    public String toString() {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < selectors.length; i++) {
            if (i != 0) {
                switch (relationships[i - 1]) {
                    case child:      result.append(" > "); break;
                    case descendent: result.append(" ");   break;
                    default: throw new IllegalStateException();
                }
            }
            result.append(selectors[i]);
        }
        return result.toString();
    }
}