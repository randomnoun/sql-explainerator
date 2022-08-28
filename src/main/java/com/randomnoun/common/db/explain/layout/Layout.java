package com.randomnoun.common.db.explain.layout;

import java.awt.Color;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.log4j.Logger;

import com.randomnoun.common.db.explain.enums.AccessTypeEnum;
import com.randomnoun.common.db.explain.graph.Shape;
import com.randomnoun.common.db.explain.graph.CShape;
import com.randomnoun.common.db.explain.json.AttachedSubqueriesNode;
import com.randomnoun.common.db.explain.json.CostInfoNode;
import com.randomnoun.common.db.explain.json.DuplicatesRemovalNode;
import com.randomnoun.common.db.explain.json.GroupingOperationNode;
import com.randomnoun.common.db.explain.json.NestedLoopNode;
import com.randomnoun.common.db.explain.json.Node;
import com.randomnoun.common.db.explain.json.OrderingOperationNode;
import com.randomnoun.common.db.explain.json.QueryBlockNode;
import com.randomnoun.common.db.explain.json.QuerySpecificationNode;
import com.randomnoun.common.db.explain.json.TableNode;
import com.randomnoun.common.db.explain.json.UnionResultNode;
import com.randomnoun.common.db.explain.visitor.RangeShapeVisitor;

/** Converts a hierarchy of Nodes into a hierarchy of Shapes */

public class Layout {

	Logger logger = Logger.getLogger(Layout.class);
	
	private QueryBlockNode topNode;
	
	public Layout(QueryBlockNode topNode) {
		this.topNode = topNode;
	}

	public Shape getLayoutShape() {
		Shape b = layout(topNode, "query_block", true);
		// probably need to reposition everything so that it starts at 0,0
		return b;
	}

	private <T> Stream<T> reverseStream(List<T> children) {
		// this is the sort of thing I would have expected to find on 
		//   https://stackoverflow.com/questions/24010109/java-8-stream-reverse-order 
		// but didn't
		return IntStream.range(0, children.size())
			.mapToObj(i -> children.get(children.size() - i - 1)); 
	}

	private String siUnits(long v) {
		DecimalFormat df =  new DecimalFormat("0.##");
		if (v < 1_000) { return String.valueOf(v); }
		else if (v < 1_000_000)                  { return df.format((double) v/1_000) + "K"; }
		else if (v < 1_000_000_000)              { return df.format((double) v/1_000_000) + "M"; }
		else if (v < 1_000_000_000_000L)         { return df.format((double) v/1_000_000_000) + "G"; }
		else if (v < 1_000_000_000_000_000L)     { return df.format((double) v/1_000_000_000_000L) + "T"; }
		else if (v < 1_000_000_000_000_000_000L) { return df.format((double) v/1_000_000_000_000_000L) + "P"; }
		else { return String.valueOf(v); }
	}
		

	private Shape layout(QueryBlockNode n, String queryBlockLabel) {
		return layout(n, queryBlockLabel, false);
	}
	
	private Shape layout(QueryBlockNode n, String queryBlockLabel, boolean topNode) {
		Node c = n.getQueryNode(); // 1 child only
		
		Shape ob = new CShape(); // outer shape
		Shape cb = null; // child shape
		int w = 100, h = 0;
		if (c != null) {
			if (c instanceof UnionResultNode) { cb = layout((UnionResultNode) c); }
			else if (c instanceof DuplicatesRemovalNode) { cb = layout((DuplicatesRemovalNode) c); }
			else if (c instanceof TableNode) { cb = layout((TableNode) c); }
			else if (c instanceof OrderingOperationNode) { cb = layout((OrderingOperationNode) c); }
			else if (c instanceof GroupingOperationNode) { cb = layout((GroupingOperationNode) c); }
			else {
				throw new IllegalStateException("unexpected class " + c.getClass().getName() + " in QueryBlockNode");
			}

			w = cb.getWidth();
			h = cb.getHeight();
		}
		ob.setSize(w, h + (queryBlockLabel == null ? 0 : 30 + 30));
		
		if (queryBlockLabel != null) {
			String label = queryBlockLabel + (topNode ? "" : " #" + n.getSelectId()); // n.selectId == null || n.selectId == 1
			String clazz = (topNode ? " topNode" : ""); // n.selectId == null || n.selectId == 1
			String tooltip = "Select ID: " + n.getSelectId() + "\n" +
			  (n.getCostInfo() == null ? "" :"Query cost: " + n.getCostInfo().getQueryCost() + "\n");
			
			
			Shape lb = new Shape(); // label shape
			lb.setCssClass("queryBlock" + clazz);
			if (cb == null) {
				lb.setParentAndPosition(ob, 0, 0);
				ob.setEdgeStartPosition(50, 0);
			} else {
				lb.setParentAndPosition(ob, cb.getEdgeStartX() - 50, 0);
				ob.setEdgeStartPosition(cb.getEdgeStartX(), 0);
			}
			lb.setSize(100, 30);
			lb.setLabel(label); 
			lb.setTooltip(tooltip);
			// lb.setFill(Color.LIGHT_GRAY);
			
			if (cb == null) {
				Shape ntb = new CShape(); // label shape
				ntb.setCssClass("tableName" + clazz);
				ntb.setParentAndPosition(ob, 0, 35);
				ntb.setSize(100, 10);
				ntb.setLabel("No tables used"); 
				ntb.setTooltip(tooltip);
				
			} else {
				cb.connectTo(lb, "s");
				cb.setParentAndPosition(ob, 0, 60);
			}
		} else if (cb != null) {
			cb.setParentAndPosition(ob, 0, 0);
		} else {
			throw new IllegalStateException("drawQueryBlock = false and no child block present");
		}
		return ob;
	}
	
	private Shape layout(UnionResultNode n) {
		List<QuerySpecificationNode> qsnList = n.getQuerySpecifications();
		
		Shape ob = new CShape(); // outer shape
		Shape cb = new CShape(); // connector shape
		
		List<Shape> qsShapes = reverseStream(qsnList)
			.map(c -> layout(c, "query_block"))
			.collect(Collectors.toList());
		
		List<Integer> tableWidths = qsShapes.stream().map(b -> b.getWidth()).collect(Collectors.toList());
		List<Integer> tableHeights = qsShapes.stream().map(b -> b.getHeight()).collect(Collectors.toList());
		int totalWidth = tableWidths.stream().mapToInt(i -> i).sum() 
			+ (qsShapes.size() - 1) * 50; // 50px padding
		int maxHeight = tableHeights.stream().mapToInt(i -> i).max().orElse(0);

		ob.setSize(totalWidth,  50 + maxHeight);
		
		cb.setParentAndPosition(ob, 0, 0);
		cb.setSize(totalWidth,  60); 
		
		Shape b = new Shape();
		b.setParentAndPosition(cb,  0,  0);
		b.setLabel("UNION");
		b.setFill(new Color(179, 179, 179)); // #b3b3b3
		b.setSize(totalWidth, 30);
		int offset = 0;
		for (Shape qsb : qsShapes) {
			qsb.setParentAndPosition(ob, offset, 50);
			int w = qsb.getWidth();
			qsb.connectTo(cb, "sv"); // , offset + (w / 2), 30 // hrm. how does this work then
			offset += w + 20;
		}
		ob.setEdgeStartPosition(totalWidth / 2, 0);
		
		return ob;
	}
	
	private Shape layout(AttachedSubqueriesNode n) {
		// @TODO multiple qsns
		List<QuerySpecificationNode> qsnList = Collections.singletonList(n.getQuerySpecification());
		
		Shape ob = new CShape(); // outer shape
		Shape cb = new CShape(); // connector shape
		
		List<Shape> qsShapes = reverseStream(qsnList)
			.map(c -> layout(c, "subquery"))
			.collect(Collectors.toList());
		
		List<Integer> tableWidths = qsShapes.stream().map(b -> b.getWidth()).collect(Collectors.toList());
		List<Integer> tableHeights = qsShapes.stream().map(b -> b.getHeight()).collect(Collectors.toList());
		int totalWidth = tableWidths.stream().mapToInt(i -> i).sum() 
			+ (qsShapes.size() - 1) * 50; // 50px padding
		int maxHeight = tableHeights.stream().mapToInt(i -> i).max().orElse(0);

		ob.setSize(totalWidth,  30 + maxHeight);
		
		cb.setParentAndPosition(ob, 0, 0);
		cb.setSize(totalWidth,  30); 
		
		Shape b = new Shape();
		b.setParentAndPosition(cb,  0,  0);
		b.setLabel("attached_subqueries");
		// b.setFill(new Color(0, 0, 0)); // #b3b3b3
		b.setSize(totalWidth, 30);
		b.setStrokeDashArray(Arrays.asList(new String[] { "2" }));
		int offset = 0;
		for (Shape qsb : qsShapes) {
			qsb.setParentAndPosition(ob, offset, 30);
			int w = qsb.getWidth();
			qsb.connectTo(cb, "sv"); // , offset + (w / 2), 30 // hrm. how does this work then
			offset += w + 20;
		}
		ob.setEdgeStartPosition(0, 15);
		
		return ob;
	}
	
	
	private Shape layout(DuplicatesRemovalNode n) {
		NestedLoopNode nln = n.getNestedLoop(); // 1 child only
		Shape qb = layout(nln);
		
		int w = qb.getWidth();
		int h = qb.getHeight();
		
		Shape ob = new CShape(); // outer shape
		ob.setSize(w, h + 10 + 45);
		ob.setEdgeStartPosition(qb.getEdgeStartX(),  0);

		Shape cb = new CShape(); // container Shape
		cb.setParentAndPosition(ob,  qb.getEdgeStartX() - 40, 0);
		cb.setSize(80, n.getUsingTemporaryTable() ? 55 : 40);
		
		Shape lb = new Shape(); // label shape
		lb.setParentAndPosition(ob, qb.getEdgeStartX() - 40, 0);
		lb.setCssClass("duplicatesRemoval");
		lb.setLabel("DISTINCT"); 
		lb.setSize(80, 40);
		
		if (n.getUsingTemporaryTable()) {
			Shape ttShape = new CShape(); 
			ttShape.setLabel("tmp table");
			ttShape.setSize(80, 10);
			ttShape.setCssClass("tempTableName");
			ttShape.setTextAnchor("start");
			ttShape.setParentAndPosition(ob, qb.getEdgeStartX() - 40, 45);
		}

		qb.connectTo(cb, "s"); // "up", 50, 30
		qb.setParentAndPosition(ob, 0, 40);
		
		return ob;

	}
	
	private Shape layout(NestedLoopNode n) {
		List<Shape> nestedLoopShapes = new ArrayList<Shape>();
		List<TableNode> qsnList = n.getTables();
		
		List<Shape> tableShapes = qsnList.stream() // reverseStream(qsnList)
			.map(c -> layout(c))
			.collect(Collectors.toList());
		List<Integer> tableWidths = tableShapes.stream().map(b -> b.getWidth()).collect(Collectors.toList());
		List<Integer> tableHeights = tableShapes.stream().map(b -> b.getHeight()).collect(Collectors.toList());
		
		int totalWidth = tableWidths.stream().mapToInt(i -> i).sum() 
			+ (tableShapes.size() - 1) * 50; // 50px padding
		int maxHeight = tableHeights.stream().mapToInt(i -> i).max().orElse(0);

		Shape ob = new CShape(); // outer shape
		ob.setSize(totalWidth, 50 + 60 + 50 + maxHeight); // arrow + diamond + arrow + tables
		
		int offset = 0;
		for (int i = 0; i < tableShapes.size(); i++) {
			Shape tb = tableShapes.get(i);
			tb.setParentAndPosition(ob, offset, 50 + 60 + 50);
			offset += tableWidths.get(i) + 50;
		}
		 
				
		Shape prevNestedLoopShape = null;
		for (int i = 1 ; i < tableShapes.size(); i++) {
			Shape tb = tableShapes.get(i);
			Shape b = new Shape(); 
			TableNode qsn = n.getTables().get(i);
			b.setShape("nestedLoop");
			b.setSize(60, 60); // diamond
			b.setParentAndPosition(ob, tb.getPosX() + tb.getEdgeStartX() - 30, 50); // centered above table beneath it
			b.setTooltip("nested_loop\n\n" +
			   "Prefix Cost: " + qsn.getCostInfo().getPrefixCost());
			nestedLoopShapes.add(b);
			
			Shape lb = new CShape(); // label shape
			lb.setCssClass("queryCost");
			lb.setParentAndPosition(b, -10, -15);
			lb.setLabel(String.valueOf(qsn.getCostInfo().getPrefixCost())); 
			lb.setTextAnchor("start");
			lb.setSize(40, 10);

			if (i == tableShapes.size() - 1) {
				lb = new CShape(); // label shape
				lb.setCssClass("queryCost");
				lb.setParentAndPosition(b, 40, -10);
				lb.setLabel(siUnits(qsn.getRowsProducedPerJoin()) +
					(qsn.getRowsProducedPerJoin() == 1 ? " row" : " rows")); 
				lb.setTextAnchor("start");
				lb.setSize(25, 10);
			} else {
				lb = new CShape(); // label shape
				lb.setCssClass("queryCost");
				lb.setParentAndPosition(b, 65, 15);
				lb.setLabel(siUnits(qsn.getRowsProducedPerJoin()) +
					(qsn.getRowsProducedPerJoin() == 1 ? " row" : " rows")); 
				lb.setTextAnchor("start");
				lb.setSize(25, 10);
			}

			
			if (i == 1) {
				Shape firstTableShape = tableShapes.get(0);
				firstTableShape.connectTo(b, "w"); // "upRight", 0, 15
				firstTableShape.setConnectedWeight((double) qsnList.get(0).getRowsExaminedPerScan());
			} else {
				prevNestedLoopShape.connectTo(b, "w"); // "right", 0, 15
				prevNestedLoopShape.setConnectedWeight((double) qsnList.get(i - 1).getRowsExaminedPerScan()); // @TODO not sure about this either
				prevNestedLoopShape.setEdgeStartPosition(60, 30);
			}
			
			Shape tableShape = tableShapes.get(i);
			tableShape.setConnectedWeight((double) qsnList.get(i).getRowsExaminedPerScan());
			tableShape.connectTo(b, "s"); // "up", 15, 30
			
			prevNestedLoopShape = b;
		}

		ob.setEdgeStartPosition(prevNestedLoopShape.getPosX() + 30, 50); // although the edge has already been drawn, so this is the edge end position really. maybe not. 
		return ob;
	}
			
	private Shape layout(TableNode n) {
		
		int w = (n.getAccessType()==AccessTypeEnum.FULL_TABLE_SCAN ? 100 :
			(n.getAccessType()==AccessTypeEnum.FULL_INDEX_SCAN ? 100 :
			(n.getAccessType()==AccessTypeEnum.NON_UNIQUE_KEY ? 150 :
			(n.getAccessType()==AccessTypeEnum.UNIQUE_KEY ? 125 : 100)))); 
		int h = 60;
		Shape ob = new CShape(); // outer shape
		
		if (n.getMaterialisedFromSubquery() == null) {
			if (n.getTableName() != null) {
				Shape lb = new CShape(); // label shape
				lb.setCssClass("tableName");
				lb.setParentAndPosition(ob, 0, 32);
				lb.setLabel(n.getTableName()); 
				lb.setSize(w, 14);
			}
			if (n.getKey() != null) {
				Shape lb = new CShape(); // label shape
				lb.setCssClass("tableKey"); 
				lb.setParentAndPosition(ob, 0, 46);
				lb.setLabel(n.getKey()); 
				lb.setSize(w, 14);
			}
		} else {
			QueryBlockNode queryBlock = n.getMaterialisedFromSubquery().getQueryBlock();
			Shape qb;
			qb = layout(queryBlock, null); // query_blocks in materialised queries aren't drawn for some reason
			// reset to 0,0

			RangeShapeVisitor rv = new RangeShapeVisitor();
			qb.traverse(rv);
			logger.info("materialised subquery range [" + rv.getMinX() + ", " + rv.getMinY() + "] - [" + rv.getMaxX() + ", " + rv.getMaxY() + "]");
			// qb.posX -= rv.getMinX();
			// qb.posY -= rv.getMinY();

			h = 80 + qb.getHeight() + 20; // 20px padding bottom
			w = Math.max(w, qb.getWidth() + 20); // 10px padding left and right

			Shape lb = new CShape();
			lb.setCssClass("dotted"); // dotted line Shape
			lb.setStroke(new Color(140, 140, 140)); // #8c8c8c
			lb.setStrokeDashArray(Arrays.asList(new String[] { "4" }));
			lb.setParentAndPosition(ob, 0, 0);
			lb.setSize(w, h);
			
			// qb after lb in the diagram so that tooltips work
			qb.setParentAndPosition(ob, 10 - rv.getMinX(), 85);

			if (n.getTableName() != null) {
				lb = new CShape(); // label shape
				lb.setCssClass("materialisedTableName");
				lb.setFill(new Color(232, 232, 232));
				lb.setParentAndPosition(ob, 0, 32);
				lb.setLabel(n.getTableName() + " (materialised)"); 
				lb.setSize(w, 20);
			}
			if (n.getKey() != null) {
				lb = new CShape(); // label shape
				lb.setCssClass("tableKey"); 
				lb.setParentAndPosition(ob, 0, 52);
				lb.setLabel(n.getKey()); 
				lb.setSize(w, 14);
			}

		}

		ob.setEdgeStartPosition(w / 2, 0);
		
		Shape b = new Shape(); 
		b.setParentAndPosition(ob, 0, 0);
		b.setSize(w, 30);
		// b.setTextColor(Color.WHITE);
		b.setCssClass("table" + (n.getAccessType()==AccessTypeEnum.FULL_TABLE_SCAN ? " fullTableScan" :
			(n.getAccessType()==AccessTypeEnum.FULL_INDEX_SCAN ? " fullIndexScan" :
			(n.getAccessType()==AccessTypeEnum.NON_UNIQUE_KEY ? " nonUniqueKey" :
			(n.getAccessType()==AccessTypeEnum.UNIQUE_KEY ? " uniqueKey" : "")))));
		b.setLabel((n.getAccessType()==AccessTypeEnum.FULL_TABLE_SCAN ? "Full Table Scan" :
			(n.getAccessType()==AccessTypeEnum.FULL_INDEX_SCAN ? "Full Index Scan" :
			(n.getAccessType()==AccessTypeEnum.NON_UNIQUE_KEY ? " Non-Unique Key Lookup" :
			(n.getAccessType()==AccessTypeEnum.UNIQUE_KEY ? "Unique Key Lookup" : "")))));
		
		CostInfoNode costInfo = n.getCostInfo();
		if (costInfo != null) {
			double cost = (costInfo.getEvalCost() == null ? (double) 0 : costInfo.getEvalCost()) +
				(costInfo.getReadCost() == null ? (double) 0 : costInfo.getReadCost());
			Shape lb = new CShape(); // label shape
			lb.setCssClass("lhsQueryCost"); lb.setTextAnchor("start");
			lb.setParentAndPosition(ob, 0, -15);
			DecimalFormat df = new DecimalFormat("0.##");
			lb.setLabel(df.format(cost)); 
			lb.setSize(w/2, 10);
		}
		if (n.getRowsExaminedPerScan() != null) {
			Shape lb = new CShape(); // label shape
			lb.setCssClass("rhsQueryCost");  lb.setTextAnchor("end");
			lb.setParentAndPosition(ob, w/2, -15);
			lb.setLabel(siUnits(n.getRowsExaminedPerScan()) + 
				(n.getRowsExaminedPerScan() == 1 ? " row" : " rows")); 
			lb.setSize(w/2, 10);
		}
		
		if (n.getAttachedSubqueries() != null) {
			Shape qb = layout(n.getAttachedSubqueries());
			qb.setParentAndPosition(ob, w + 50, 0);
			qb.connectTo(b, "e");
			w = w + 50 + qb.getWidth();
			h = Math.max(h,  qb.getHeight());
		
		}
		
		ob.setSize(w, h);
		return ob;
		
	}
	private Shape layout(OrderingOperationNode n) {

		NestedLoopNode nln = n.nestedLoop; // 1 child only
		Shape cb = layout(nln);
		
		int w = cb.getWidth();
		int h = cb.getHeight();
		
		Shape ob = new CShape(); // outer shape, nestedLoop exit edge is drawn inside the nestedLoop Shape
		ob.setSize(w, h + 40);
		ob.setEdgeStartPosition(cb.getEdgeStartX(),  0);
		
		if (n.isUsingTemporaryTable()) { 
			Shape tb = new CShape(); // label shape
			tb.setParentAndPosition(ob, cb.getEdgeStartX() - 40, 45); 
			tb.setSize(w, 10);
			tb.setCssClass("tempTableName");
			tb.setLabel("tmp table"); 
		}
		
		Shape lb = new Shape(); // label shape
		lb.setParentAndPosition(ob, cb.getEdgeStartX() - 40, 0);
		lb.setCssClass("orderingOperation");
		lb.setLabel("ORDER"); 
		lb.setSize(80, 40);
		lb.setTooltip("Ordering operation\n\n" +
			"Using Filesort: " + n.isUsingFilesort());

		cb.connectTo(lb, "s"); // "up", 50, 30
		cb.setParentAndPosition(ob, 0, 40);
		
		
		return ob;
		
	}
	private Shape layout(GroupingOperationNode n) {
		
		NestedLoopNode nln = n.getNestedLoop(); // 1 child only
		Shape cb = layout(nln);
		
		int w = cb.getWidth();
		int h = cb.getHeight();
		
		Shape ob = new CShape(); // outer shape 
		ob.setSize(w, h + 40);
		ob.setEdgeStartPosition(cb.getEdgeStartX(),  0);
		
		Shape lb = new Shape(); // label shape
		lb.setParentAndPosition(ob, cb.getEdgeStartX() - 40, 0);
		lb.setCssClass("groupingOperation");
		lb.setLabel("GROUP"); 
		lb.setSize(80, 40);

		cb.connectTo(lb, "s"); // "up", 50, 30
		cb.setParentAndPosition(ob, 0, 40);
		
		return ob;
		
		
	}
	
	private Shape layout(QuerySpecificationNode n, String queryBlockLabel) {

		QueryBlockNode qb = n.getQueryBlock();
		
		Shape cb = layout(qb, queryBlockLabel);
		
		int w = cb.getWidth();
		int h = cb.getHeight();
		
		Shape ob = new CShape(); 
		ob.setSize(w, h + 40);
		ob.setEdgeStartPosition(cb.getEdgeStartX(),  40);

		cb.setParentAndPosition(ob, 0, 40);
		return ob;
	}
	
	private Shape layout(Node n) {
		throw new UnsupportedOperationException("layout for node " + n.getClass().getName() + " not implemented");
	}
	
	
}