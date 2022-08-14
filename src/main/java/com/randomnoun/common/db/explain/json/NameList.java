package com.randomnoun.common.db.explain.json;

import java.util.ArrayList;

import com.randomnoun.common.Struct;
import com.randomnoun.common.Text;

public class NameList extends ArrayList<String> implements Struct.ToJson {

	/** generated serialVersionUID */
	private static final long serialVersionUID = -4398170610222717666L;

	@Override
	public String toJson() {
		String s = "[";
		for (int i=0; i<this.size(); i++) {
			String n = this.get(i);
			s += (i==0 ? "" : ", ") + "\"" + Text.escapeJavascript(n) + "\"";
		}
		s += "]";
		return s;
	}
}