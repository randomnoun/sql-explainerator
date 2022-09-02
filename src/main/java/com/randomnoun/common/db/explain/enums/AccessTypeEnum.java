package com.randomnoun.common.db.explain.enums;

public enum AccessTypeEnum {
	
	FULL_TABLE_SCAN("ALL", "fullTableScan", "Full Table Scan",  
	  "Very High - very costly for large tables (not so much for small ones).\n" +
      "No usable indexes were found for the table and the optimizer must search every row.\n" +
	  "This could also mean the search range is so broad that the index would be useless."), // red
	
	FULL_INDEX_SCAN("index", "fullIndexScan", "Full Index Scan",  
	  "High - especially for large indexes"), // red
	
	NON_UNIQUE_KEY("ref", "nonUniqueKey", "Non-Unique Key Lookup",  
	  "Low-medium - Low if number of matching rows is small, higher as the number of rows increases"), // green
	
	UNIQUE_KEY("eq_ref", "uniqueKey", "Unique Key Lookup",  
	  "Low - The optimizer is able to find an index that it can use to retrieve required records.\n" +
	  "Fast because the index search leads directly to the page with all the row data"),
	
	CONST("const", "singleRow", "Single Row\n(constant)",  
	  "Very low cost"); // blue

	; // green
	
	
	private String jsonValue;
	private String label;
	private String cssClass;
	private String costHint;
	
	private AccessTypeEnum(String jsonValue, String cssClass, String label, String costHint) {
		this.jsonValue = jsonValue;
		this.cssClass = cssClass;
		this.label = label;
		this.costHint = costHint;
	}
	
	public static AccessTypeEnum fromJsonValue(String jsonValue) {
		for (AccessTypeEnum e : AccessTypeEnum.values()) {
			if (e.jsonValue.equals(jsonValue)) { return e; }
		}
		throw new IllegalArgumentException("no AccessTypeEnum with jsonValue '" + jsonValue + "'");
	}
	public String getJsonValue() { 
		return jsonValue;
	}
	public String getLabel() {
		return label;
	}
	public String getCssClass() {
		return cssClass;
	}
	public String getCostHint() {
		return costHint;
	}

}