package com.sun.stylesheet.css.parser;

public class SimpleNode implements Node {
    protected Node parent;
    protected Node[] children;
    protected Token firstToken;
    protected Token lastToken;
    protected int id;
    protected CSSParserImpl parser;

    public SimpleNode(int i) {
        id = i;
    }

    public SimpleNode(CSSParserImpl p, int i) {
        this(i);
        parser = p;
    }
    
    public int getId() {
        return id;
    }
    
    public void jjtOpen() {
    }

    public void jjtClose() {
    }
  
    public void jjtSetParent(Node n) { parent = n; }
    public Node jjtGetParent() { return parent; }

    public void jjtAddChild(Node n, int i) {
        if (children == null) {
            children = new Node[i + 1];
        } 
        else if (i >= children.length) {
            Node c[] = new Node[i + 1];
            System.arraycopy(children, 0, c, 0, children.length);
            children = c;
        }
        children[i] = n;
    }
  
    public SimpleNode getChild(int i) {
        return (SimpleNode) jjtGetChild(i);
    }

    public Node jjtGetChild(int i) {
        return children[i];
    }

    public int jjtGetNumChildren() {
        return (children == null) ? 0 : children.length;
    }

    private void appendSpecialTokens(StringBuffer s, Token st) {
        if (st != null) {
            appendSpecialTokens(s, st.specialToken);
            s.append(st.image) ; 
        } 
    } 
    
    public String getText() {
	    StringBuffer text = new StringBuffer();
	    Token t = firstToken;
	    while (t != null) {
	        appendSpecialTokens(text, t.specialToken);
		    text.append(t.image);
		    if (t == lastToken)
			    break;
		    t = t.next;
	    }
	    
    	return text.toString().trim();
    }

/* You can override these two methods in subclasses of SimpleNode to
     customize the way the node appears when the tree is dumped.  If
     your output uses more than one line you should override
     toString(String), otherwise overriding toString() is probably all
     you need to do. */

  public String toString() { return CSSParserImplTreeConstants.jjtNodeName[id]; }
  public String toString(String prefix) { return prefix + toString(); }

  /* Override this method if you want to customize how the node dumps
     out its children. */

  public void dump(String prefix) {
    System.out.println(toString(prefix));
    if (children != null) {
      for (int i = 0; i < children.length; ++i) {
	SimpleNode n = (SimpleNode)children[i];
	if (n != null) {
	  n.dump(prefix + " ");
	}
      }
    }
  }
}

