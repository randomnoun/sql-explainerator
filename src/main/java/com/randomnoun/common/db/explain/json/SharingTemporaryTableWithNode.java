package com.randomnoun.common.db.explain.json;

// can appear within MaterialisedFromSubqueryNodes
public class SharingTemporaryTableWithNode extends Node {
	
	public SharingTemporaryTableWithNode() {
		super("sharing_temporary_table_with", false);
	}

	private Long selectId;

	public Long getSelectId() {
		return selectId;
	}

	public void setSelectId(Long selectId) {
		this.selectId = selectId;
	}
	
}