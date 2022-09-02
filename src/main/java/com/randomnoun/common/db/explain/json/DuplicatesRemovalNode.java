package com.randomnoun.common.db.explain.json;

public class DuplicatesRemovalNode extends Node {
	private Boolean usingTemporaryTable;
	private CostInfoNode costInfo;
	
	private NestedLoopNode nestedLoop;
	
	public DuplicatesRemovalNode() {
		super("duplicates_removal", false); 
	}
	public Boolean getUsingTemporaryTable() {
		return usingTemporaryTable;
	}
	public void setUsingTemporaryTable(Boolean usingTemporaryTable) {
		this.usingTemporaryTable = usingTemporaryTable;
	}
	public NestedLoopNode getNestedLoop() {
		return nestedLoop;
	}
	public void setNestedLoop(NestedLoopNode nestedLoop) {
		this.nestedLoop = nestedLoop;
	}
	public CostInfoNode getCostInfo() {
		return costInfo;
	}
	public void setCostInfo(CostInfoNode costInfo) {
		this.costInfo = costInfo;
	}
	
}