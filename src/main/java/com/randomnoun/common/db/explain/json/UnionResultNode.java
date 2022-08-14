package com.randomnoun.common.db.explain.json;

import java.util.List;

public class UnionResultNode extends Node {
	public String tableName;
	public List<QuerySpecificationNode> querySpecifications;
	public UnionResultNode() {
		super("union_result", false); // TODO subtypes
	}
	

}