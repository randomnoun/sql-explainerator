package com.randomnoun.common.db.explain.json;

import java.util.ArrayList;
import java.util.List;

public class NestedLoopNode extends Node {
	
	private List<TableNode> tables = new ArrayList<>();
	
	public NestedLoopNode() {
		super("nestedLoop", true);
	}

	public List<TableNode> getTables() {
		return tables;
	}

	public void setTables(List<TableNode> tables) {
		this.tables = tables;
	}
	
}