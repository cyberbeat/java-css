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

/* Modified by Volker Härtel, 8 Dec 2011 */ package com.sun.stylesheet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.sun.stylesheet.css.CompoundSelector;
import com.sun.stylesheet.css.SimpleSelector;

/**
 * A set of modifications which can affect a {@link Styleable}.  Rules should 
 * examine the object to determine whether or not they apply, and if so should 
 * use {@link PropertyManager} to make any needed modifications.  <b>Rules 
 * should not directly modify the object</b>;  they must always go through 
 * <code>PropertyManager</code> so that it can properly enforce rule priorities 
 * and perform cleanup when a stylesheet is removed.
 *
 *@author Ethan Nicholas
 */
public class Rule {
    private Stylesheet parent;
    private Selector[] selectors;
    private transient Map<Object, PseudoclassListener> pseudoclassListeners;
    private Declaration[] declarations;

    
    /** 
     * Creates a <code>Rule</code> with the specified selectors and 
     * properties.  While it is possible to create a <code>Rule</code> with 
     * zero selectors, it will never match anything.
     *
     *@param selectors the selectors which determine whether or not the rule 
     *      matches
     *@param declarations the properties to apply when the rule matches
     *@throws NullPointerException if any argument is null
     */
    public Rule(Selector[] selectors, Declaration[] declarations) {
        if (selectors == null || declarations == null)
            throw new NullPointerException();
        this.parent = parent;
        this.selectors = selectors;
        this.declarations = declarations;
    }


    /** 
     * Returns the <code>Stylesheet</code> to which this rule belongs. 
     *
     *@return the stylesheet to which this rule belongs
     */
    public Stylesheet getStylesheet() {
        return parent;
    }


    /** 
     * Sets the <code>Stylesheet</code> to which this rule belongs.  This method
     * is called automatically when the rule is added to a stylesheet.
     *
     *@param stylesheet the stylesheet to which this rule belongs
     */
    public void setStylesheet(Stylesheet stylesheet) {
        this.parent = stylesheet;
    }


    /**
     * Returns the selectors which determine whether or not this rule matches.
     *
     *@return an array of {@link Selector Selectors}
     */
    public Selector[] getSelectors() {
        return selectors;
    }
    
    
    /**
     * Returns the declarations which are applied when this rule matches.
     *
     *@return an array of {@link Declaration Declarations}
     */
    public Declaration[] getDeclarations() {
        return declarations;
    }


    private PseudoclassListener getPseudoclassListener(final Styleable object, 
            final Stylesheet sourceSheet, final Priority priority, 
            final int count) {
        if (pseudoclassListeners == null)
            pseudoclassListeners = new HashMap<Object, PseudoclassListener>();
        List key = new ArrayList();
        key.add(object);
        key.add(sourceSheet);
        PseudoclassListener result = pseudoclassListeners.get(key);
        if (result == null) {
            result = new PseudoclassListener() {
                private int currentCount;
                private Priority important;
                
                public void pseudoclassAdded(PseudoclassEvent e) {
                    if (++currentCount == count) {
                        for (Declaration d : declarations) {
                            if (d.isImportant()) {
                                if (important == null) {
                                    important = (Priority) priority.clone();
                                    important.setImportant(true);
                                }
                                d.applyTo(object, Rule.this, important);
                            }
                            else    
                                d.applyTo(object, Rule.this, priority);
                        }
                        PropertyManager.cascadeFrom(object);
                    }
                }
    
                
                public void pseudoclassRemoved(PseudoclassEvent e) {
                    if (currentCount-- == count) {
                        for (Declaration d : declarations) {
                            Priority currentPriority;
                            if (d.isImportant()) {
                                if (important == null) {
                                    important = (Priority) priority.clone();
                                    important.setImportant(true);
                                }
                                currentPriority = important;
                            }
                            else
                                currentPriority = priority;
                            d.removeFrom(object, 
                                    Rule.this,
                                    currentPriority);
                        }
                        PropertyManager.cascadeFrom(object);
                    }
                }
            };
            pseudoclassListeners.put(key, result);
        }
        return result;
    }
    
    
    /**
     * Applies the rule to an object.  The rule will apply its selectors to the 
     * object to determine whether or not it applies, and if so use {@link 
     * PropertyManager} to apply the needed changes.
     * <p>
     * The <tt>depth</tt> and <tt>index</tt> arguments will be passed to 
     * <tt>PropertyManager#applyProperty</tt> so that it can determine priority, 
     * but are not used by <code>Rule</code> itself.
     *
     *@param object the object to style
     *@param depth the number of times {@link Styleable#getStyleableParent} 
     *      would have to be called before reaching the style root
     *@param index the index of this rule within the stylesheet
     */
    public void applyTo(final Styleable object, int depth, int index) 
            throws StylesheetException {
        Match[] matches = matches(object);
        if (matches != null) {
            for (Match match : matches) {
                Match.Pseudoclass[] pseudoclasses = match.getPseudoclasses();
                Priority priority = new Priority(false, 
                        pseudoclasses != null ? pseudoclasses.length : 0,
                        parent.getPriority(), depth, match.getIdCount(), 
                        match.getStyleClassCount(), 
                        match.getJavaClassWeight(),
                        index);
                Priority important = null;
                if (pseudoclasses == null || pseudoclasses.length == 0) {
                    for (Declaration d : declarations) {
                        if (d.isImportant()) {
                            if (important == null) {
                                important = (Priority) priority.clone();
                                important.setImportant(true);
                            }
                            d.applyTo(object, this, important);
                        }
                        else    
                            d.applyTo(object, this, priority);
                    }
                }
                else {
                    for (Match.Pseudoclass pseudoclass : pseudoclasses) {
                        PropertyManager.addPseudoclassListener(
                                pseudoclass.getStyleable(),
                                pseudoclass.getName(), 
                                getPseudoclassListener(object, parent, priority, 
                                        pseudoclasses.length), 
                                this);
                    }
                }
            }
        }
    }


    /**
     * Checks all selectors for a match, returning an array of all relevant 
     * matches.  A match is considered irrelevant if its presence or absence 
     * cannot affect whether or not the rule applies;  this means that among 
     * static (non-pseudoclass) matches, only the highest priority one is 
     * relevant, and among pseudoclass matches, only ones with higher priority 
     * than the most specific static match are relevant.
     *
     *@param node the object to test against
     *@return an array of all relevant matches, or <code>null</code> if none
     */
    public Match[] matches(Styleable node) {
        Match staticMatch = null;
        List<Match> pseudoclassMatches = null;
        for (int i = 0; i < selectors.length; i++) {
            Match match = selectors[i].matches(node);
            if (match != null) {
                Match.Pseudoclass[] pseudoclasses = match.getPseudoclasses() ;
                if (pseudoclasses == null || pseudoclasses.length == 0) {
                    // static match, compare it to the current highest static 
                    // match
                    if (staticMatch == null)
                        staticMatch = match;
                    else if (match.compareTo(staticMatch) > 0)
                        staticMatch = match;
                }
                else if (staticMatch == null || 
                        match.compareTo(staticMatch) > 0) {
                    // pseudoclass match, only add it if it outranks the current 
                    // highest static match
                    if (pseudoclassMatches == null)
                        pseudoclassMatches = new ArrayList<Match>();
                    pseudoclassMatches.add(match);
                }
            }
        }
        if (pseudoclassMatches != null && staticMatch != null) {
            // remove pseudoclass matches which are outranked by the static 
            // match
            Iterator<Match> i = pseudoclassMatches.iterator();
            while (i.hasNext()) {
                if (i.next().compareTo(staticMatch) <= 0)
                    i.remove();
            }
            if (pseudoclassMatches.size() == 0)
                pseudoclassMatches = null;
        }
        
        // done, produce result array
        if (pseudoclassMatches != null && pseudoclassMatches.size() > 0) {
            if (staticMatch != null)
                pseudoclassMatches.add(staticMatch);
            return pseudoclassMatches.toArray(
                    new Match[pseudoclassMatches.size()]);
        }
        else
            return staticMatch != null ? new Match[] { staticMatch } : null;
    }


    void writeBinary(DataOutputStream out) throws IOException {
        out.writeByte(selectors.length);
        for (Selector selector : selectors) {
            if (selector instanceof SimpleSelector) {
                out.write(1);
                ((SimpleSelector) selector).writeBinary(out);
            }
            else if (selector instanceof CompoundSelector) {
                out.write(2);
                ((CompoundSelector) selector).writeBinary(out);
            }
            else
                throw new IOException("cannot serialize selector: " + selector);
        }
        out.writeByte(declarations.length);
        for (Declaration declaration : declarations)
            declaration.writeBinary(out);
    }
    
    
    static Rule readBinary(DataInputStream in) throws IOException {
        int selectorCount = in.readByte();
        Selector[] selectors = new Selector[selectorCount];
        for (int i = 0; i < selectorCount; i++) {
            int selectorType = in.read();
            switch (selectorType) {
                case 1: selectors[i] = SimpleSelector.readBinary(in); break;
                case 2: selectors[i] = CompoundSelector.readBinary(in); break;
                default: throw new IOException("expected 1 or 2, found " +
                        selectorType);
            }
        }
        int declarationCount = in.readByte();
        Declaration[] declarations = new Declaration[declarationCount];
        for (int i = 0; i < declarationCount; i++)
            declarations[i] = Declaration.readBinary(in);
        Rule result = new Rule(selectors, declarations);
        return result;
    }
    

    /** Converts this object to a string. */
    public String toString() {
        StringBuilder result = new StringBuilder();
        for (Selector selector : selectors) {
            if (result.length() > 0)
                result.append(", ");
            result.append(selector);
        }
        result.append(" {\n");
        for (Declaration declaration : declarations) {
            result.append("  ");
            result.append(declaration);
            result.append(";\n");
        }
        result.append('}');
        return result.toString();
    }
}