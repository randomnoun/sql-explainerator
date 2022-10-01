package com.randomnoun.common.db.explain.layout;

import com.randomnoun.common.db.explain.graph.Shape;
import com.randomnoun.common.db.explain.json.QueryBlockNode;

public interface Layout {

	void setQueryBlockNode(QueryBlockNode qbn);

	Shape getLayoutShape();

}
