package com.randomnoun.common.db.explain.json;

import java.util.ArrayList;
import java.util.List;

public class WindowsNode extends Node {
	
	private List<WindowNode> windowNodes = new ArrayList<>();
	
	public WindowsNode() {
		super("windows", true);
	}

	public List<WindowNode> getWindows() {
		return windowNodes;
	}

	public void addWindow(WindowNode windowNode) {
		this.windowNodes.add(windowNode);
	}
	
}