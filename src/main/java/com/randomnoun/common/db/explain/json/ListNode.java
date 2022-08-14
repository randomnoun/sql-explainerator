package com.randomnoun.common.db.explain.json;

import java.util.List;

public class ListNode extends Node {
	public ListNode(String name, List<? extends Node> children) {
		super(name, true);
		for (Node n : children) {
			this.getChildren().add(n);
		}
	}
}