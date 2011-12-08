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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

import com.sun.stylesheet.css.parser.CSSParser;
import com.sun.stylesheet.types.TypeManager;

/**
 * Displays the styles in effect for all components and allows stylesheets to be 
 * edited.
 *
 *@author Ethan Nicholas
 */
public class DebugWindow extends JFrame {
    private static class StylesheetEntry {
        private String name;
        private String text;
        private Stylesheet stylesheet;
        private boolean enabled = true;
        
        public StylesheetEntry(String name, String text, 
                Stylesheet stylesheet) {
            this.name = name;
            this.text = text;
            this.stylesheet = stylesheet;
        }
        
        
        public String toString() {
            return name;
        }
    }
    
    
    private JTabbedPane tabs;
    private JTextArea text;
    private JList list;
    private JTable table;
    private Set<Styleable> nodes = new HashSet<Styleable>();
    private StylesheetEntry current;
    
    public DebugWindow() {
        super("Stylesheet Editor");
        
//        tabs = createTabbedPane();
//        add(tabs);
        add(createStylesheetTab());
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        
        setSize(600, 400);
        setLocation(300, 0);
    }
    
    
    private JTabbedPane createTabbedPane() {
        JTabbedPane result = new JTabbedPane();
        result.addTab("Stylesheets", createStylesheetTab());
        result.addTab("Components", createComponentsTab());
        return result;
    }
    
    
    private JComponent createStylesheetTab() {
        JPanel result = new JPanel();
        result.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
    
        JSplitPane split = new JSplitPane();
                
        text = new JTextArea();
        split.setLeftComponent(new JScrollPane(text));
        
        list = new JList(new DefaultListModel());
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        split.setRightComponent(new JScrollPane(list));

        c.fill = GridBagConstraints.BOTH;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 1;
        c.weighty = 1;
        c.insets = new Insets(3, 3, 3, 3);
        split.setDividerLocation(450);
        result.add(split, c);
        
        Box bottom = new Box(BoxLayout.X_AXIS);
        final JCheckBox enabled = new JCheckBox("Enable Stylesheet");
        enabled.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                try {
                    StylesheetEntry value = (StylesheetEntry) 
                            list.getSelectedValue();
                    if (value != null) {
                        Stylesheet stylesheet = value.stylesheet;
                        if (enabled.isSelected()) {
                            CSSParser.parse(text.getText(), stylesheet);
                            stylesheet.reapply();
                        }
                        else {
                            stylesheet.removeFromAll();
                        }
                    }
                }
                catch (StylesheetException ex) {
                    reportException(ex);
                }
            }
        });
        enabled.setSelected(true);
        bottom.add(enabled);

        list.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                if (current != null)
                    current.text = text.getText();
                current = (StylesheetEntry) list.getSelectedValue();
                text.setText(current != null ? current.text : "");
                enabled.setSelected(current != null ? current.enabled : false);
            }
        });
        
        JButton update = new JButton("Update");
        update.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                DefaultListModel model = (DefaultListModel) list.getModel();
                try {
                    Stylesheet stylesheet = ((StylesheetEntry) 
                            list.getSelectedValue()).stylesheet;
                    CSSParser.parse(text.getText(), stylesheet);
                    stylesheet.reapply();
                    enabled.setSelected(true);
                }
                catch (Throwable ex) {
                    reportException(ex);
                }
            }
        });
        bottom.add(update);
        
        c.fill = GridBagConstraints.NONE;
        c.weightx = 0;
        c.weighty = 0;
        result.add(bottom, c);
        return result;
    }
    
    
    private class PropertyEntry {
        private PropertyManager.PropertyValue value;
        private String text;
    
        public PropertyEntry(PropertyManager.PropertyValue value, String text) {
            this.value = value;
            this.text = text;
        }
        
        
        public String toString() {
            return text;
        }
    }
    
    
    private JComponent createComponentsTab() {
        JSplitPane result = new JSplitPane();
        
        final TableCellRenderer classRenderer = new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable table, 
                    Object value, boolean isSelected, boolean hasFocus, int row, 
                    int column) {
                Styleable s = (Styleable) value;
                String text = s.getObjectClasses()[0].getName();
                text = text.substring(text.lastIndexOf(".") + 1);
                return super.getTableCellRendererComponent(table, text, 
                        isSelected, hasFocus, row, column);
            }
        };
        DefaultTableModel model = new DefaultTableModel(new String[] { "Class", 
                "ID", "Style Class" }, 0) {
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(model) {
            public TableCellRenderer getCellRenderer(int row, int column) {
                if (column == 0)
                    return classRenderer;
                else
                    return super.getCellRenderer(row, column);
            }
        };
//        table.setRowSorter(new TableRowSorter(model));
        result.setLeftComponent(new JScrollPane(table));
        
        final JList propertyList = new JList();
        JSplitPane subsplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        subsplit.setLeftComponent(new JScrollPane(propertyList));
        final JTextArea details = new JTextArea();
        details.setLineWrap(true);
        details.setWrapStyleWord(true);
        subsplit.setRightComponent(new JScrollPane(details));
        propertyList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                PropertyEntry entry = (PropertyEntry) 
                        propertyList.getSelectedValue();
                if (entry != null) {
                    details.setText(entry.value.getPriority() + "\n" + 
                            entry.value.getSource());
                }
                else
                    details.setText("");
            }
        });
        
        result.setRightComponent(subsplit);
        result.setDividerLocation(300);
        
        table.getSelectionModel().addListSelectionListener(
            new ListSelectionListener() {
                public void valueChanged(ListSelectionEvent e) {
                    DefaultListModel model = new DefaultListModel();
                    Styleable selected = (Styleable) 
                            table.getModel().getValueAt(table.getSelectedRow(), 
                            0);
                    List<PropertyManager.PropertyValue> properties = 
                            PropertyManager.getAllPropertiesForObject(selected);
                    Set<String> completed = new HashSet<String>();
                    for (PropertyManager.PropertyValue property : properties) {
                        String name = property.getPropertyName();
                        if (!completed.contains(name)) {
                            completed.add(name);
                            List<PropertyManager.PropertyValue> values = 
                                    PropertyManager.getPropertyListForObject(
                                    selected, name);
                            PropertyManager.PropertyValue active = values.get(
                                    values.size() - 1);
                            model.addElement(new PropertyEntry(active, name + 
                                    " = " + active.getValue()));
                            for (int i = values.size() - 2; i >= 0; i--) {
                                PropertyManager.PropertyValue value = 
                                        values.get(i);
                                model.addElement(new PropertyEntry(value, 
                                        "<html><font color=#888888><i>" + 
                                        "(overridden)</i> " + 
                                        value.getValue() + "</font>"));
                            }
                        }
                    }
                    propertyList.setModel(model);
                }
            }
        );
        
        return result;
    }
    
    
    private void reportException(Throwable e) {
        StringWriter buffer = new StringWriter();
        e.printStackTrace(new PrintWriter(buffer));
        JScrollPane scrollPane = new JScrollPane(
                new JTextArea(buffer.toString()));
        scrollPane.setPreferredSize(new Dimension(550, 300));
        JOptionPane.showMessageDialog(this, scrollPane);
    }
    
    
    public void addStylesheet(Stylesheet stylesheet) {
        ((DefaultListModel) list.getModel()).addElement(
                new StylesheetEntry("<unnamed>", "", stylesheet));
        if (list.getSelectedIndex() == -1)
            list.setSelectedIndex(0);
    }
    
    
    public void setText(Stylesheet stylesheet, String text) {
        DefaultListModel model = (DefaultListModel) list.getModel();
        for (int i = 0; i < model.size(); i++) {
            StylesheetEntry entry = (StylesheetEntry) model.get(i);
            if (entry.stylesheet == stylesheet) {
                entry.text = text;
                if (list.getSelectedIndex() == i)
                    this.text.setText(text);
            }
        }
    }


    public void setName(Stylesheet stylesheet, String name) {
        DefaultListModel model = (DefaultListModel) list.getModel();
        for (int i = 0; i < model.size(); i++) {
            StylesheetEntry entry = (StylesheetEntry) model.get(i);
            if (entry.stylesheet == stylesheet) {
                entry.name = name;
            }
        }
    }
    
    
    public void stylesheetApplied(Stylesheet stylesheet, Styleable node) {
        if (nodes.contains(node))
            return;
        Styleable parent = node.getStyleableParent();
        Styleable self = TypeManager.getStyleable(this);
        while (parent != null) {
            if (parent == self)
                return;
            parent = parent.getStyleableParent();
        }
/*        DefaultTableModel model = (DefaultTableModel) table.getModel();
        final int index = model.getRowCount();
        model.addRow(new Object[] { node, node.getID(), node.getStyleClass() });
        nodes.add(node);
        
        if (node instanceof DefaultStyleable) {
            DefaultStyleable ds = (DefaultStyleable) node;
            if (ds.getBaseObject() instanceof Component) {
                ((Component) ds.getBaseObject()).addMouseListener(
                    new MouseAdapter() {
                        @Override
                        public void mousePressed(MouseEvent e) {
                            tabs.setSelectedIndex(1);
                            table.getSelectionModel().setSelectionInterval(
                                    index, index);
                        }
                    }
                );
            }
        }*/
    }


    public void stylesheetRemoved(Stylesheet stylesheet, Styleable node) {
    }
}