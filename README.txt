This is a modified version of JAVA-CSS, which was originally found here: http://java.net/projects/javacss/

Java-CSS is about styling swing GUIs with CSS-like stylesheets. It works quite well, and can be combined with layoutmanagers like miglayout for example.

It was introduced here http://weblogs.java.net/blog/2008/07/17/introducing-java-css by Ethan Nicholas.

I removed javafx-support and fixed a memory leak.

Link above on archive.org:

https://web.archive.org/web/20150512202732/http://weblogs.java.net/blog/2008/07/17/introducing-java-css

Example from above page:

Before:

JSlider slider = new JSlider(0, 100, currentValue);
slider.setMinorTickSpacing(5);
slider.setMajorTickSpacing(25);
slider.setPaintTicks(true);
slider.setForeground(DEFAULT_FOREGROUND);

With CSS:

* {
  font: Dialog-12;
  foreground: black;
}

JSlider#mySlider {
  minorTickSpacing: 5;
  majorTickSpacing: 25;
  paintTicks: true;
}

JSlider#tip:{value <= 10} {
  background: red !over 0.3s;
}

Admittedly, in this simple example there's not a clear benefit to using CSS
instead of a proprietary file -- the syntax is a bit different, but the two
files are about equally complex. The real benefit of CSS comes from all of the
additional power it offers.

Want to only affect sliders which appear under MyColorChooser? No problem, use
the selector "MyColorChooser JSlider". Or pick every label serving
as a title with "JLabel.title". What about JButtons, but only when
they are moused over? Use "JButton:mouseover".

What about even more complex selectors, such as "JSliders which are direct
children of a MyPanel appearing in the currently active window, but only when
they are set to their maximum value"? No problem, with a slight syntax
extension "Window:active MyPanel > JSlider:{value == maximum}" does
just the trick.

The stylesheet specifies some interesting rules, such as

JSlider#tip:{value <= 10} {
  background: red !over 0.3s;
}
The JSlider#tip portion of the selector means that this rule
only applies to the JSlider named "tip". Okay, simple enough. The
next part ":{value <= 10}" is what is called a programmatic
pseudoclass -- basically just a boolean expression which controls whether
or not the rule applies, taking the form of an EL expression evaluated using Beans
Binding. So this rule applies only when the slider has a value of 10 or les
