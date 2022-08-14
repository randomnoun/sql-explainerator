package com.randomnoun.common.db.explain.json;

import java.util.ArrayList;
import java.util.List;

public class NestedLoopNode extends Node {
	
	public List<TableNode> tables = new ArrayList<>();
	
	public NestedLoopNode() {
		super("nestedLoop", true);
	}
	
}