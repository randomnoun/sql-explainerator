package com.randomnoun.common.db.explain.enums;

public enum TooltipTypeEnum {
	
	/** No tooltips */
	NONE("none"),
	
	/** Tooltips contained in SVG &lt;title&gt; tags */
	SVG_TITLE("title"),  
	
	/** Tooltips contained in SVG attributes, with some javascript to display them */
	ATTRIBUTE_JS("javascript"), 
	
	/** Tooltips contained in SVG attributes only ( the containing page will supply tooltip javascript ) */
	ATTRIBUTE("attribute");

	private String value;
	private TooltipTypeEnum(String value) {
		this.value = value;
	}
	public String getValue() {
		return value;
	}
	public static TooltipTypeEnum fromValue(String value) {
		for (TooltipTypeEnum e : TooltipTypeEnum.values()) {
			if (e.value.equals(value)) { return e; }
		}
		throw new IllegalArgumentException("no TooltipTypeEnum with value '" + value + "'");
	}
}