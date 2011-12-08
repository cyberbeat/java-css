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

/* Modified by Volker Härtel, 8 Dec 2011 */ package com.sun.stylesheet.css.parser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.sun.stylesheet.Animation;
import com.sun.stylesheet.DebugWindow;
import com.sun.stylesheet.Declaration;
import com.sun.stylesheet.Rule;
import com.sun.stylesheet.Selector;
import com.sun.stylesheet.Stylesheet;
import com.sun.stylesheet.css.CompoundSelector;
import com.sun.stylesheet.css.SimpleSelector;
import com.sun.stylesheet.types.Time;
import com.sun.stylesheet.types.TypeManager;

public class CSSParser {
    private CSSParser() { }
    
    
	/** 
	 * Creates a stylesheet from a CSS document string. 
	 *
	 *@param stylesheetText the CSS document to parse
	 *@return the parsed stylesheet
	 *@throws ParseException if a parse error occurs
	 */
	public static Stylesheet parse(String stylesheetText) 
	        throws ParseException {
		return parse(new StringReader(stylesheetText));
	}


	/** 
	 * Re-parses a stylesheet from a CSS document string. 
	 *
	 *@param stylesheetText the CSS document to parse
	 *@param stylesheet the stylesheet to be re-parsed
	 *@throws ParseException if a parse error occurs
	 */
	public static void parse(String stylesheetText, Stylesheet stylesheet) 
	        throws ParseException {
	    parse(new StringReader(stylesheetText), stylesheet);
    }


	/** 
	 * Creates a stylesheet by reading a CSS document. 
	 *
	 *@param in the document to parse
	 *@return the parsed stylesheet
	 *@throws ParseException if a parse error occurs
	 */
	public static Stylesheet parse(Reader in) throws ParseException {
        Stylesheet result = new Stylesheet();
        parse(in, result);
        return result;
	}


	/** 
	 * Re-parses a stylesheet by reading a CSS document. 
	 *
	 *@param in the document to parse
	 *@param stylesheet the stylesheet to re-parse
	 *@throws ParseException if a parse error occurs
	 */
	public static void parse(Reader in, Stylesheet stylesheet) 
	        throws ParseException {
	    DebugWindow debugWindow = Stylesheet.getDebugWindow();
	    if (debugWindow != null) {
	        try {
	            StringWriter out = new StringWriter();
	            char[] buffer = new char[1024];
	            int c;
	            while ((c = in.read(buffer)) > 0)
	                out.write(buffer, 0, c);
	            String text = out.toString();
	            in = new StringReader(text);
	            debugWindow.setText(stylesheet, text);
	        }
	        catch (IOException e) {
	            throw new ParseException(e.toString());
	        }
	    }
	    CSSParserImpl p = new CSSParserImpl(in);
	    SimpleNode node = p.Stylesheet();
	    List<Rule> newRules = new ArrayList<Rule>();
	    for (int i = 0; i < node.jjtGetNumChildren(); i++) {
	        SimpleNode ruleNode = node.getChild(i);
	        newRules.add(processRule(ruleNode));
	    }
		List<Rule> oldRules = stylesheet.getRules();
		oldRules.clear();
		oldRules.addAll(newRules);
    }
    
    
	/**
	 * Creates a stylesheet by reading a CSS document from a URL, assuming
	 * UTF-8 encoding.
	 *
	 *@param the URL to parse
	 *@return the parsed stylesheet
	 *@throws ParseException if a parse error occurs
	 */
	public static Stylesheet parse(URL url) throws ParseException, IOException {
	    InputStream in = url.openStream();
	    Reader reader = new InputStreamReader(in, "UTF-8");
	    try {
			return parse(reader);
		}
		finally {
	    	reader.close();
		}
	}


	/**
	 * Updates the given stylesheet by reading a CSS document from a URL,
         * assuming UTF-8 encoding.
	 *
	 *@param the URL to parse
         *@param the stylesheet
	 *@throws ParseException if a parse error occurs
	 */
	public static void parse(URL url, Stylesheet stylesheet) throws ParseException, IOException {
	    InputStream in = url.openStream();
	    Reader reader = new InputStreamReader(in, "UTF-8");
	    try {
			parse(reader, stylesheet);
		}
		finally {
	    	reader.close();
		}
	}
        
        
	/**
	 * Creates a stylesheet by reading a CSS document from a URL, assuming
	 * the specified encoding.
	 *
	 *@param the URL to parse
	 *@param encoding the CSS document's encoding
	 *@return the parsed stylesheet
	 *@throws ParseException if a parse error occurs
	 *@throws UnsupportedEncodingException if the encoding is not supported
	 */
	public static Stylesheet parse(URL url, String encoding) 
	        throws ParseException, UnsupportedEncodingException, IOException {
	    InputStream in = url.openStream();
	    Reader reader = new InputStreamReader(in, encoding);
		try {
	    	return parse(reader);
		}
		finally {
	    	reader.close();
		}
	}


	/** 
	 * Processes the parse tree for a simple selector into a SimpleSelector. 
	 */
	private static Selector processSimpleSelector(SimpleNode selector) {
	    if (selector.getId() != CSSParserImplTreeConstants.JJTSIMPLESELECTOR)
	        throw new IllegalArgumentException("argument node is not a " + 
	                "SimpleSelector (" + selector.getId() + ")");
	    String javaClassName = null;
	    String styleClass = null;
	    List<String> pseudoclasses = null;
	    String id = null;

	    for (int i = 0; i < selector.jjtGetNumChildren(); i++) {
	        SimpleNode child = selector.getChild(i);
	        switch (child.getId()) {
	            case CSSParserImplTreeConstants.JJTJAVACLASS: 
	                if (!child.getText().equals("*")) 
	                    javaClassName = child.getText(); 
	                break;
                
	            case CSSParserImplTreeConstants.JJTCLASS: 
	                styleClass = child.getText().substring(1); 
	                break;
                
	            case CSSParserImplTreeConstants.JJTPSEUDOCLASS: 
	                if (pseudoclasses == null)
	                    pseudoclasses = new ArrayList<String>();
	                pseudoclasses.add(child.getText().substring(1)); 
	                break;
                
	            case CSSParserImplTreeConstants.JJTID: 
	                id = child.getText().substring(1); 
	                break;
            
	            default: 
	                throw new IllegalStateException("unexpected child " + 
	                        "of Selector node, type=" + child.getId());
	        }
	    }
    
	    return new SimpleSelector(javaClassName, styleClass, 
	            pseudoclasses != null ?
	                pseudoclasses.toArray(new String[pseudoclasses.size()]) : 
	                null, 
	            id);
	}


	/** Processes the parse tree for a selector into a Selector. */
	private static Selector processSelector(SimpleNode selectorNode) {    
	    if (selectorNode.getId() != CSSParserImplTreeConstants.JJTSELECTOR)
	        throw new IllegalArgumentException("argument node is not a " +
	                "Selector");

	    List<Selector> simpleSelectors = new ArrayList<Selector>();
	    for (int i = 0; i < selectorNode.jjtGetNumChildren(); i += 2) {
	        SimpleNode simpleSelectorNode = selectorNode.getChild(i);
	        simpleSelectors.add(processSimpleSelector(simpleSelectorNode));
	    }
	    if (simpleSelectors.size() == 1)
	        return simpleSelectors.get(0);
	    else {
	        Selector[] selectors = simpleSelectors.toArray(
	                new Selector[simpleSelectors.size()]);
	        CompoundSelector.Relationship[] relationships = 
	                new CompoundSelector.Relationship[selectors.length - 1];
	        for (int i = 1; i < selectorNode.jjtGetNumChildren(); i += 2) {
	            String relationship = selectorNode.getChild(i).getText().trim();
	            if (relationship.equals(">"))
	                relationships[i / 2] = 
	                        CompoundSelector.Relationship.child;
	            else if (relationship.equals(""))
	                relationships[i / 2] = 
	                        CompoundSelector.Relationship.descendent;
	            else
	                throw new Error("can't-happen error: found relationship '" + 
	                        relationship + "'");
	        }
	        return new CompoundSelector(selectors, relationships);
	    }
	}
	

	private static String camelCase(String string) {
	    int pos = string.indexOf("-");
	    if (pos == -1)
	        return string;
	    StringBuilder result = new StringBuilder(string.substring(0, pos));
	    for (;;) {
	        if (pos == string.length() - 1) {
	            result.append('-');
	            break;
	        }
	        else {
	            result.append(Character.toUpperCase(string.charAt(pos + 1)));
	            pos += 2;
	            int next = string.indexOf("-", pos);
	            if (next != -1) {
	                result.append(string.substring(pos, next));
	                pos = next;
	            }
	            else {
	                result.append(string.substring(pos));
	                break;
	            }
	        }
	    }
	    return result.toString();
    }


	/** Processes the parse tree for a rule into a Rule. */
	private static Rule processRule(SimpleNode ruleNode) {    
	    if (ruleNode.getId() != CSSParserImplTreeConstants.JJTRULE)
	        throw new IllegalArgumentException("argument node is not a Rule");
	    SimpleNode selectorsNode = ruleNode.getChild(0);
	    assert selectorsNode.getId() == CSSParserImplTreeConstants.JJTSELECTORS : 
	            "expected node to be of type Selectors";
    
	    List<Selector> selectors = new ArrayList<Selector>();
	    for (int i = 0; i < selectorsNode.jjtGetNumChildren(); i++) {
	        SimpleNode selectorNode = selectorsNode.getChild(i);
	        selectors.add(processSelector(selectorNode));
	    }
    
	    List<Declaration> declarations = new ArrayList<Declaration>();
	    for (int i = 1; i < ruleNode.jjtGetNumChildren(); i++) {
	        SimpleNode declarationNode = ruleNode.getChild(i);
	        if (declarationNode.getId() ==
	                CSSParserImplTreeConstants.JJTDECLARATION) {
	            String key = declarationNode.getChild(0).getText();
	            key = camelCase(key);
	            SimpleNode valueNode = declarationNode.getChild(1);
	            String value = valueNode.getText();
	            boolean important = false;
	            Animation animation = null;
	            if (declarationNode.jjtGetNumChildren() > 2) {
	                SimpleNode extrasNode = declarationNode.getChild(2);
        	        assert extrasNode.getId() ==
        	                CSSParserImplTreeConstants.JJTDECLEXTRAS :
        	                "expected node to be of type DeclExtras";
            	    for (int j = 0; j < extrasNode.jjtGetNumChildren(); j++) {
            	        SimpleNode extraNode = extrasNode.getChild(j);
            	        String extraText = extraNode.getText();
            	        if (extraText.equals("important"))
            	            important = true;
            	        else if (extraText.startsWith("over")) {
            	            String duration = extraNode.getChild(0).getText();
            	            Time time = (Time) TypeManager.convertFromString(
            	                    duration, Time.class);
            	            Animation.Interpolation interpolation = 
            	                    Animation.Interpolation.DEFAULT_CURVE;
            	            if (extraNode.jjtGetNumChildren() > 1) {
            	                String interpolationKey = 
            	                        extraNode.getChild(1).getText();
            	                if (interpolationKey.equals("default"))
            	                    interpolation = 
            	                            Animation.Interpolation.DEFAULT_CURVE;
            	                else if (interpolationKey.equals("linear"))
            	                    interpolation = 
            	                            Animation.Interpolation.LINEAR;
            	                else if (interpolationKey.equals("ease-in"))
            	                    interpolation = 
            	                            Animation.Interpolation.EASE_IN;
            	                else if (interpolationKey.equals("ease-out"))
            	                    interpolation = 
            	                            Animation.Interpolation.EASE_OUT;
            	                else if (interpolationKey.equals("ease-in-out"))
            	                    interpolation = 
            	                            Animation.Interpolation.EASE_IN_OUT;
        	                }
            	            animation = new Animation(time, interpolation);
        	            }
            	    }
	            }
	            declarations.add(new Declaration(key, value, important,
	                    animation));
	        }
	    }

	    Rule rule = new Rule(
	            selectors.toArray(new Selector[selectors.size()]), 
	            declarations.toArray(new Declaration[declarations.size()]));
	    return rule;
	}
}