package com.randomnoun.common.db.explain.json;

import com.randomnoun.common.Struct;

public class CostInfo implements Struct.ToJson {

	private Double queryCost;
	private Double readCost;
	private Double evalCost;
	private Double prefixCost;
	private String dataReadPerJoin; // "96K"
	
	
	public String toJson() {
		// return "{ \"let's pretend\": \"this is the costInfo\" }";
		String s = "{";
		boolean f = true;
		if (dataReadPerJoin!=null) { s += (f ? "" : ", ") + "\"dataReadPerJoin\":\"" + dataReadPerJoin + "\""; f = false; }
		if (queryCost!=null) { s += (f ? "" : ", ") + "\"queryCost\":" + queryCost; f = false; }
		if (readCost!=null) { s += (f ? "" : ", ") + "\"readCost\":" + readCost; f = false; }
		if (evalCost!=null) { s += (f ? "" : ", ") + "\"evalCost\":" + evalCost; f = false; }
		if (prefixCost!=null) { s += (f ? "" : ", ") + "\"prefixCost\":" + prefixCost; f = false; }
		s += "}";
		return s;
	}

	public Double getQueryCost() {
		return queryCost;
	}
	public void setQueryCost(Double queryCost) {
		this.queryCost = queryCost;
	}
	public Double getReadCost() {
		return readCost;
	}
	public void setReadCost(Double readCost) {
		this.readCost = readCost;
	}
	public Double getEvalCost() {
		return evalCost;
	}
	public void setEvalCost(Double evalCost) {
		this.evalCost = evalCost;
	}
	public Double getPrefixCost() {
		return prefixCost;
	}
	public void setPrefixCost(Double prefixCost) {
		this.prefixCost = prefixCost;
	}
	public String getDataReadPerJoin() {
		return dataReadPerJoin;
	}
	public void setDataReadPerJoin(String dataReadPerJoin) {
		this.dataReadPerJoin = dataReadPerJoin;
	}

}