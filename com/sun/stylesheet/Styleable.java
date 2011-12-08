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

import java.util.Map;

import com.sun.stylesheet.swing.ComponentStyleSupport;

/**
 * Base interface for all objects which can be affected by stylesheets.  In 
 * order to be styled, objects must either themselves implement 
 * <code>Styleable</code> or have a wrapper class registered with {@link 
 * com.sun.css.types.TypeManager TypeManager}.
 *
 *@author Ethan Nicholas
 */
public interface Styleable {
    /**
     * Returns the ID of the object, as used in CSS ID selectors.  ID selectors
     * are of the form <code>#idstring</code> and match all objects with the 
     * specified ID.
     *
     *@return the object's ID
     */
    String getID();
    
    /**
     * Returns an array of all Java classes which should be considered matches 
     * for this object.  Normally this includes the base object class as well as 
     * all implemented interfaces.
     *
     *@return the Java classes of the object being styled
     */
    Class[] getObjectClasses();
    
    /**
     * Returns the style class of the object, as used in CSS class selectors.  
     * Class selectors are of the form <code>.style</code> and match all objects 
     * with the specified style class.
     *
     *@return the style class
     */
    String getStyleClass();
    
    /**
     * Informs the object that a new stylesheet is being applied to it.  The 
     * <code>depth</code> parameter indicates the "ancestry level" of the style 
     * root (the object to which the stylesheet is being directly applied);  for 
     * instance if the style root is this object's parent, the depth is 1.  The 
     * depth is needed to correctly resolve priorities in the face of 
     * conflicting rules, with lower depths having precedence over higher
     * depths.  Generally the depth is only useful as an argument to various 
     * methods in {@link PropertyManager} which perform these priority 
     * resolutions.
     * <p>
     * Objects are not required to do anything in response to this method; it is 
     * largely informational.  {@link ComponentStyleSupport} keeps track of 
     * stylesheets affecting it so that newly-added children can be styled 
     * automatically.
     *
     *@param s the stylesheet being applied
     *@param depth the depth of this object below the style root
     *@see #removeStylesheet
     */
    void addStylesheet(Stylesheet s, int depth);

    /**
     * Informs the object that a stylesheet no longer applies to it. The 
     * <code>depth</code> parameter indicates the "ancestry level" of the style 
     * root (the object to which the stylesheet is being directly applied);  for 
     * instance if the style root is this object's parent, the depth is 1.  The 
     * depth is needed to correctly resolve priorities in the face of 
     * conflicting rules, with lower depths having precedence over higher 
     * depths.  Generally the depth is only useful as an argument to various 
     * methods in {@link PropertyManager} which perform these priority 
     * resolutions.
     * <p>
     * Objects are not required to do anything in response to this method; it is 
     * largely informational.  {@link ComponentStyleSupport} keeps track of
     * stylesheets affecting it so that newly-added children can be styled 
     * automatically.
     *
     *@param s the stylesheet being applied
     *@param depth the depth of this object below the style root
     *@see #addStylesheet
     */
    void removeStylesheet(Stylesheet s, int depth);

    /**
     * Converts a property from a string representation (as specified in a CSS 
     * document) to its actual value.  Styleable objects should generally 
     * determine the type of the property and then use {@link 
     * com.sun.css.types.TypeManager TypeManager} to perform the actual 
     * conversion.
     *
     *@param propertyName the name of the property being assigned
     *@param value a string representation of the data as specified in a CSS 
     *      document
     *@return the converted value
     *@throws StylesheetException if the value could not be converted
     */
    Object convertPropertyFromString(String propertyName, String value) 
            throws StylesheetException;

    /**
     * Returns the current value of the specified property.
     *
     *@param key the name of the property to return
     *@return the value of the property
     *@throws StylesheetException if the property does not exist or could not be read
     *@see #setProperty
     */
    Object getProperty(String key) throws StylesheetException;

    /**
     * Changes a property of the object.  <code>setProperty</code> should 
     * normally only be called directly by {@link com.sun.css.PropertyManager} 
     * so as to correctly handle priority resolution and stylesheet removal.
     *
     *@param key the name of the property to set
     *@param value the new value of the property
     *@throws StylesheetException if the property does not exist or could not be
     *      written
     *@see #getProperty
     */
    void setProperty(String key, Object value) throws StylesheetException;
    
    /**
     * Returns the children of this object.  When <code>Styleable</code> 
     * wrappers are used, it is essential that the same wrapper instance always 
     * be used for the same object.  {@link
     * com.sun.css.types.TypeManager#getStyleable TypeManager.getStyleable()} is 
     * the preferred way to obtain <code>Styleable</code> wrappers, as it 
     * automatically ensures that the same wrapper is reused for the same 
     * object.
     *
     *@return an array of the object's children, which may be <code>null</code>
     */
    Styleable[] getStyleableChildren();
    
    /**
     * Returns the parent of this object.  When <code>Styleable</code> wrappers 
     * are used, it is essential that the same wrapper instance always be used 
     * for the same object.  {@link
     * com.sun.css.types.TypeManager#getStyleable TypeManager.getStyleable()} is 
     * the preferred way to obtain <code>Styleable</code> wrappers, as it 
     * automatically ensures that the same wrapper is reused for the same 
     * object.
     *
     *@return this object's parent, which may be <code>null</code>
     */
    Styleable getStyleableParent();
    
    /**
     * Adds a listener which will be notified as the specified pseudoclass is 
     * added to or removed from this object.  For example, {@link 
     * java.awt.Component Components} support the <code>mouseover</code> 
     * pseudoclass, and adding a <code>PseudoclassListener</code> with the
     * <code>mouseover</code> pseudoclass will cause it to receive notifications
     * as the mouse enters and exits the component.
     * <p>
     * <b>Important:</b> if the pseudoclass is <i>already in effect</i> when
     * this method is called (i.e. adding a listener for "enabled" to a button
     * which is currently enabled) this method is responsible for immediately
     * calling <code>pseudoclassAdded()</code> on the listener.
     *
     *@param pseudoclass the pseudoclass to listen for
     *@param listener the listener which will receive pseudoclass notifications
     *@throws StylesheetException if the specified pseudoclass is not supported by this 
     *      object
     *@see #removePseudoclassListener
     */
    void addPseudoclassListener(String pseudoclass, 
            PseudoclassListener listener) throws StylesheetException;

    /**
     * Removes a pseudoclass listener from this object.
     *
     *@param pseudoclass the pseudoclass which was being listened for
     *@param listener the listener to remove
     *@throws StylesheetException if the specified pseudoclass is not supported by this
     *      object
     *@see #addPseudoclassListener
     */
    void removePseudoclassListener(String pseudoclass, 
            PseudoclassListener listener) throws StylesheetException;

    /**
     * Returns <code>true</code> if the specified property should be inherited 
     * by this object when it is found on this object's parent.
     *
     *@param propertyName the name of the property to potentially inherit
     *@return <code>true</code> if the property should be inherited
     *@throws StylesheetException if an error occurs
     */
    boolean isPropertyInherited(String propertyName) throws StylesheetException;
    
    
    /**
     * Attempts to split a 'compound' property into its component pieces.  A 
     * compound property is one such as "font", which is split into "font-size", 
     * "font-weight", "font-face", and "font-style".  When queried with a 
     * compound property, this method returns a <code>Map</code> of the 
     * individual properties to their values.  When queried with a simple 
     * (non-compound) property, this method returns <code>null</code>.
     *
     *@param propertyName the name of the property to split
     *@param value the property's value
     *@return a <code>Map</code> of the individual properties and values, or 
     *      <code>null</code> for non-compound properties
     *@throws StylesheetException if an error occurs
     */
    Map<String, Object> splitCompoundProperty(String propertyName, Object value)
            throws StylesheetException;
}