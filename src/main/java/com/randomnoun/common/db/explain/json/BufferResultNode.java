package com.randomnoun.common.db.explain.json;

public class BufferResultNode extends Node {
	private QueryBlockNode queryBlock;

	public BufferResultNode() {
		super("buffer_result", false);
	}

	public QueryBlockNode getQueryBlock() {
		return queryBlock;
	}

	public void setQueryBlock(QueryBlockNode queryBlock) {
		this.queryBlock = queryBlock;
	}
}