package com.randomnoun.common.db.explain.json;

import java.util.ArrayList;
import java.util.List;

public class HavingSubqueriesNode extends Node {
	
	private List<QuerySpecificationNode> querySpecifications = new ArrayList<>();
	
	public HavingSubqueriesNode() {
		super("attached_subqueries", true);
	}

	public List<QuerySpecificationNode> getQuerySpecifications() {
		return querySpecifications;
	}

	public void addQuerySpecification(QuerySpecificationNode querySpecification) {
		this.querySpecifications.add(querySpecification);
	}
	

}