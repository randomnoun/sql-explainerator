package com.randomnoun.common.db.explain.json;

import com.randomnoun.common.db.explain.enums.AccessTypeEnum;

public class TableNode extends Node {
	public CostInfoNode costInfo;
	public AccessTypeEnum accessType;
	public String tableName;
	public String key;
	public Long rowsExaminedPerScan;
	public Long rowsProducedPerJoin;
	public AttachedSubqueriesNode attachedSubqueries;
	public MaterialisedFromSubqueryNode materialisedFromSubquery;

	public TableNode() {
		super("table", false);
	}
	
}