package com.randomnoun.common.db.explain.json;

import java.util.List;

import com.randomnoun.common.db.explain.enums.AccessTypeEnum;

public class TableNode extends Node {
	private CostInfoNode costInfo;
	private AccessTypeEnum accessType;
	private String tableName;
	private String key;
	private List<String> possibleKeys;
	private List<String> usedKeyParts;
	private List<String> usedColumns;
	private Long rowsExaminedPerScan;
	private Long rowsProducedPerJoin;
	private Double filtered;
	private AttachedSubqueriesNode attachedSubqueries;
	private MaterialisedFromSubqueryNode materialisedFromSubquery;

	public TableNode() {
		super("table", false);
	}

	public CostInfoNode getCostInfo() {
		return costInfo;
	}

	public void setCostInfo(CostInfoNode costInfo) {
		this.costInfo = costInfo;
	}

	public AccessTypeEnum getAccessType() {
		return accessType;
	}

	public void setAccessType(AccessTypeEnum accessType) {
		this.accessType = accessType;
	}

	public String getTableName() {
		return tableName;
	}

	public void setTableName(String tableName) {
		this.tableName = tableName;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public Long getRowsExaminedPerScan() {
		return rowsExaminedPerScan;
	}

	public void setRowsExaminedPerScan(Long rowsExaminedPerScan) {
		this.rowsExaminedPerScan = rowsExaminedPerScan;
	}

	public Long getRowsProducedPerJoin() {
		return rowsProducedPerJoin;
	}

	public void setRowsProducedPerJoin(Long rowsProducedPerJoin) {
		this.rowsProducedPerJoin = rowsProducedPerJoin;
	}

	public AttachedSubqueriesNode getAttachedSubqueries() {
		return attachedSubqueries;
	}

	public void setAttachedSubqueries(AttachedSubqueriesNode attachedSubqueries) {
		this.attachedSubqueries = attachedSubqueries;
	}

	public MaterialisedFromSubqueryNode getMaterialisedFromSubquery() {
		return materialisedFromSubquery;
	}

	public void setMaterialisedFromSubquery(MaterialisedFromSubqueryNode materialisedFromSubquery) {
		this.materialisedFromSubquery = materialisedFromSubquery;
	}

	public List<String> getPossibleKeys() {
		return possibleKeys;
	}

	public void setPossibleKeys(List<String> possibleKeys) {
		this.possibleKeys = possibleKeys;
	}

	public List<String> getUsedKeyParts() {
		return usedKeyParts;
	}

	public void setUsedKeyParts(List<String> usedKeyParts) {
		this.usedKeyParts = usedKeyParts;
	}

	public List<String> getUsedColumns() {
		return usedColumns;
	}

	public void setUsedColumns(List<String> usedColumns) {
		this.usedColumns = usedColumns;
	}

	public Double getFiltered() {
		return filtered;
	}

	public void setFiltered(Double filtered) {
		this.filtered = filtered;
	}

}