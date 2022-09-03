package com.randomnoun.common.db.explain.json;

public class DuplicatesRemovalNode extends Node {
	private boolean usingFilesort;
	private boolean usingTemporaryTable;
	private CostInfoNode costInfo;
	
	private NestedLoopNode nestedLoop;
	
	public DuplicatesRemovalNode() {
		super("duplicates_removal", false); 
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

	public boolean isUsingFilesort() {
		return usingFilesort;
	}

	public void setUsingFilesort(boolean usingFilesort) {
		this.usingFilesort = usingFilesort;
	}

	public boolean isUsingTemporaryTable() {
		return usingTemporaryTable;
	}

	public void setUsingTemporaryTable(boolean usingTemporaryTable) {
		this.usingTemporaryTable = usingTemporaryTable;
	}


	
}