package com.randomnoun.common.db.explain.json;

public class AttachedSubqueriesNode extends Node {
	private QuerySpecificationNode querySpecification;

	public AttachedSubqueriesNode() {
		super("attached_subqueries", true);
	}

	public QuerySpecificationNode getQuerySpecification() {
		return querySpecification;
	}

	public void setQuerySpecification(QuerySpecificationNode querySpecification) {
		this.querySpecification = querySpecification;
	}
	

}