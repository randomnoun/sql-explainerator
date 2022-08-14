package com.randomnoun.common.db.explain.enums;

public enum AccessTypeEnum {
	FULL_TABLE_SCAN("ALL"), // red
	FULL_INDEX_SCAN("index"), // red
	NON_UNIQUE_KEY("ref"), // green
	UNIQUE_KEY("eq_ref"); // green
	private String jsonValue;
	private AccessTypeEnum(String jsonValue) {
		this.jsonValue = jsonValue;
	}
	public static AccessTypeEnum fromJsonValue(String jsonValue) {
		for (AccessTypeEnum e : AccessTypeEnum.values()) {
			if (e.jsonValue.equals(jsonValue)) { return e; }
		}
		throw new IllegalArgumentException("no AccessTypeEnum with jsonValue '" + jsonValue + "'");
	}
}