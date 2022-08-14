package com.randomnoun.common.db.explain.json;

public class QuerySpecificationNode extends Node {
	private QueryBlockNode queryBlock;

	public QuerySpecificationNode() {
		super("query_specification", false);
	}

	public QueryBlockNode getQueryBlock() {
		return queryBlock;
	}

	public void setQueryBlock(QueryBlockNode queryBlock) {
		this.queryBlock = queryBlock;
	}
}