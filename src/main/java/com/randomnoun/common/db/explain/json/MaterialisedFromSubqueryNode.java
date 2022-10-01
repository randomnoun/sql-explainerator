package com.randomnoun.common.db.explain.json;

public class MaterialisedFromSubqueryNode extends Node {
	private boolean dependent;
	private boolean cacheable;
	private boolean usingTemporaryTable;
	
	private Node subquery; // either a QueryBlockNode or a SharingTemporaryTableWithNode

	public MaterialisedFromSubqueryNode() {
		super("materialized_from_subquery", false);
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

	public Node getSubquery() {
		return subquery;
	}

	public void setSubquery(Node subquery) {
		this.subquery = subquery;
	}
}