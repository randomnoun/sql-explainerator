package com.randomnoun.common.db.explain.json;

// can appear within MaterialisedFromSubqueryNodes
public class WindowingNode extends Node {
	
	WindowsNode windows;
	Node childNode; // table
	
	public WindowingNode() {
		super("windowing", false);
	}

	public WindowsNode getWindows() {
		return windows;
	}

	public void setWindows(WindowsNode windows) {
		this.windows = windows;
	}

	public Node getChildNode() {
		return childNode;
	}

	public void setChildNode(Node childNode) {
		this.childNode = childNode;
	}
	
}