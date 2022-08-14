package com.randomnoun.common.db.explain.json;

public class MaterialisedFromSubqueryNode extends Node {
	public QueryBlockNode queryBlock;
	public boolean dependent;
	public boolean cacheable;
	public boolean usingTemporaryTable;

	public MaterialisedFromSubqueryNode() {
		super("materialized_from_subquery", false);
	}
}