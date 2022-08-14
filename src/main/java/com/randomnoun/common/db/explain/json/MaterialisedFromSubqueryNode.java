package com.randomnoun.common.db.explain.json;

public class MaterialisedFromSubqueryNode extends Node {
	private QueryBlockNode queryBlock;
	private boolean dependent;
	private boolean cacheable;
	private boolean usingTemporaryTable;

	public MaterialisedFromSubqueryNode() {
		super("materialized_from_subquery", false);
	}

	public QueryBlockNode getQueryBlock() {
		return queryBlock;
	}

	public void setQueryBlock(QueryBlockNode queryBlock) {
		this.queryBlock = queryBlock;
	}

	public boolean isDependent() {
		return dependent;
	}

	public void setDependent(boolean dependent) {
		this.dependent = dependent;
	}

	public boolean isCacheable() {
		return cacheable;
	}

	public void setCacheable(boolean cacheable) {
		this.cacheable = cacheable;
	}

	public boolean isUsingTemporaryTable() {
		return usingTemporaryTable;
	}

	public void setUsingTemporaryTable(boolean usingTemporaryTable) {
		this.usingTemporaryTable = usingTemporaryTable;
	}
}