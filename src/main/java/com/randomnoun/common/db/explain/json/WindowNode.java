package com.randomnoun.common.db.explain.json;

import java.util.List;

// can appear within MaterialisedFromSubqueryNodes
public class WindowNode extends Node {
	
	String name;
	Long definitionPosition;
	boolean usingTemporaryTable;
	List<String> functions;
	
	public WindowNode() {
		super("window", false);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Long getDefinitionPosition() {
		return definitionPosition;
	}

	public void setDefinitionPosition(Long definitionPosition) {
		this.definitionPosition = definitionPosition;
	}

	public boolean isUsingTemporaryTable() {
		return usingTemporaryTable;
	}

	public void setUsingTemporaryTable(boolean usingTemporaryTable) {
		this.usingTemporaryTable = usingTemporaryTable;
	}

	public List<String> getFunctions() {
		return functions;
	}

	public void setFunctions(List<String> functions) {
		this.functions = functions;
	}

}