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
import java.util.HashMap;
import java.util.Map;

import org.jdesktop.animation.timing.Animator;
import org.jdesktop.animation.timing.TimingTargetAdapter;
import org.jdesktop.animation.timing.interpolation.SplineInterpolator;

import com.sun.stylesheet.types.Time;
import com.sun.stylesheet.types.TypeManager;

class AnimationManager {
    // todo: consolidate animators when more than one is running with the same
    // settings
    private static class AnimationKey {
        private Styleable object;
        private String property;
        
        
        public AnimationKey(Styleable object, String property) {
            this.object = object;
            this.property = property;
        }
        
        
        @Override
        public boolean equals(Object o) {
            if (!(o instanceof AnimationKey))
                return false;
            AnimationKey a = (AnimationKey) o;
            return object == a.object && property.equals(a.property);
        }
        
        
        @Override
        public int hashCode() {
            return object.hashCode() ^ property.hashCode();
        }
    }
    
    
    private static class PropertyAnimation {
        private Animator animator;
        private Object targetValue;
        
        
        public PropertyAnimation(Animator animator, Object targetValue) {
            this.animator = animator;
            this.targetValue = targetValue;
        }
        
        
        public void stop() {
            animator.stop();
        }
        
        
        public Object getTargetValue() {
            return targetValue;
        }
    }
    
    
    private static Map<AnimationKey, PropertyAnimation> animations = 
            new HashMap<AnimationKey, PropertyAnimation>();
    
    private AnimationManager() { /* not instantiable */ }
    
    static boolean isAnimating(Styleable object, String property) {
        AnimationKey key = new AnimationKey(object, property);
        PropertyAnimation current = animations.get(key);
        return current != null;
    }
    
    
    static Object getTargetValue(Styleable object, String property) {
        AnimationKey key = new AnimationKey(object, property);
        PropertyAnimation current = animations.get(key);
        return current != null ? current.getTargetValue() : null;
    }
    

    public static void animateTransition(final Styleable object, 
            final String property, Object targetValue, 
            Animation animation) {
        AnimationKey key = new AnimationKey(object, property);
        PropertyAnimation current = animations.get(key);
        if (current != null)
            current.stop();
        final Object initialValue = object.getProperty(property);
        if (targetValue == null && property.equals("background")) {
            // special-case animations to a null background: animate to the
            // parent's background, since that's the effect of a null
            Styleable parent = object.getStyleableParent();
            if (parent != null) {
                try {
                    targetValue = parent.getProperty("background");
                }
                catch (StylesheetException e) {
                }
            }
        }
        if (initialValue == null || targetValue == null) {
            System.err.println("Warning: cannot animate to or from null " +
                    "(" + object.getObjectClasses()[0] + "." + property + 
                    " from " + initialValue + " to " + targetValue + ")");
            object.setProperty(property, targetValue);
            return;
        }
        final Object finalTarget = targetValue;
        int duration = (int) animation.getDuration().getTime(Time.Unit.MS);
        Animator animator = new Animator(duration);
        Point2D controlPoint1 = animation.getControlPoint1();
        if (controlPoint1 != null) {
            Point2D controlPoint2 = animation.getControlPoint2();
            animator.setInterpolator(new SplineInterpolator(
                (float) controlPoint1.getX(),
                (float) controlPoint1.getY(),
                (float) controlPoint2.getX(),
                (float) controlPoint2.getY()));
        }
        animator.addTarget(new TimingTargetAdapter() {
            public void timingEvent(float fraction) {
                object.setProperty(property, TypeManager.interpolate(object, 
                        initialValue, 
                        finalTarget, fraction));
            }
        });
        animations.put(key, new PropertyAnimation(animator, targetValue));
        animator.start();
    }
}