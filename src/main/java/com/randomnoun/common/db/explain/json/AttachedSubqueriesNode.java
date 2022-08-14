package com.randomnoun.common.db.explain.json;

public class AttachedSubqueriesNode extends Node {
	public QuerySpecificationNode querySpecification;

	public AttachedSubqueriesNode() {
		super("attached_subqueries", true);
	}
	

}