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

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;

public class Main {
    private Main() { }
    
    private static void showUsage() {
        System.out.println("Usage: java -jar css-all.jar -in <path> [-out <path>]");
        System.out.println("Input and output files must end in either .css or .cssbin");
    }
    
    
    private static boolean isValid(String filename) {
        return filename.endsWith(".css") || filename.endsWith(".cssbin");
    }
    
    
    public static void main(String[] arg) throws Exception {
        String in = null;
        String out = null;
        
        for (int i = 0; i < arg.length; i++) {
            if (arg[i].equals("-in")) {
                if (++i < arg.length && isValid(arg[i]))
                    in = arg[i];
                else {
                    showUsage();
                    break;
                }
            }
            else if (arg[i].equals("-out")) {
                if (++i < arg.length && isValid(arg[i]))
                    out = arg[i];
                else {
                    showUsage();
                    break;
                }
            }
        }
        
        if (in != null) {
            Stylesheet stylesheet;
            if (in.endsWith(".css")) {
                Reader reader = new FileReader(in);
                stylesheet = Stylesheet.readCSS(reader);
                reader.close();
            }
            else {
                InputStream inputStream = new FileInputStream(in);
                stylesheet = Stylesheet.readBinary(inputStream);
                inputStream.close();
            }
            
            if (out != null) {
                if (out.endsWith(".css")) {
                    Writer writer = new FileWriter(out);
                    stylesheet.writeCSS(writer);
                    writer.close();
                }
                else {
                    OutputStream outputStream = new FileOutputStream(out);
                    stylesheet.writeBinary(outputStream);
                    outputStream.close();
                }
            }
            else
                System.out.println("Successfully parsed " + in);
        }
        else
            showUsage();
    }
}