package com.randomnoun.common.db.explain.json;

public class QuerySpecificationNode extends Node {
	public QueryBlockNode queryBlock;

	public QuerySpecificationNode() {
		super("query_specification", false);
	}
}