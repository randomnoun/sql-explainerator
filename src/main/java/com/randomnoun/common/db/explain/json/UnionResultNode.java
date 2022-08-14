package com.randomnoun.common.db.explain.json;

import java.util.List;

public class UnionResultNode extends Node {
	private String tableName;
	private List<QuerySpecificationNode> querySpecifications;
	public UnionResultNode() {
		super("union_result", false); // TODO subtypes
	}
	public String getTableName() {
		return tableName;
	}
	public void setTableName(String tableName) {
		this.tableName = tableName;
	}
	public List<QuerySpecificationNode> getQuerySpecifications() {
		return querySpecifications;
	}
	public void setQuerySpecifications(List<QuerySpecificationNode> querySpecifications) {
		this.querySpecifications = querySpecifications;
	}
	

}