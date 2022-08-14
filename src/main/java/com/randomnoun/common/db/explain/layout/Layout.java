package com.randomnoun.common.db.explain.layout;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.log4j.Logger;

import com.randomnoun.common.db.explain.enums.AccessTypeEnum;
import com.randomnoun.common.db.explain.graph.Box;
import com.randomnoun.common.db.explain.graph.CBox;
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
import com.randomnoun.common.db.explain.visitor.RangeVisitor;

/** Converts a hierarchy of Nodes into a hierarchy of Boxes */

public class Layout {

	Logger logger = Logger.getLogger(Layout.class);
	
	private QueryBlockNode topNode;
	
	public Layout(QueryBlockNode topNode) {
		this.topNode = topNode;
	}
	
	public Box getLayoutBox() {
		Box b = layout(topNode, "query_block", true);
		// probably need to reposition everything so that it starts at 0,0
		return b;
	}
	

	private Box layout(QueryBlockNode n, String queryBlockLabel) {
		return layout(n, queryBlockLabel, false);
	}
	
	private Box layout(QueryBlockNode n, String queryBlockLabel, boolean topNode) {
		Node c = n.getQueryNode(); // 1 child only
		
		Box ob = new CBox(); // outer box
		Box cb = null; // child box
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
			
			
			Box lb = new Box(); // label box
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
				Box ntb = new CBox(); // label box
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
	
	private Box layout(UnionResultNode n) {
		List<QuerySpecificationNode> qsnList = n.getQuerySpecifications();
		
		Box ob = new CBox(); // outer box
		Box cb = new CBox(); // connector box
		
		List<Box> qsBoxes = reverseStream(qsnList)
			.map(c -> layout(c, "query_block"))
			.collect(Collectors.toList());
		
		List<Integer> tableWidths = qsBoxes.stream().map(b -> b.getWidth()).collect(Collectors.toList());
		List<Integer> tableHeights = qsBoxes.stream().map(b -> b.getHeight()).collect(Collectors.toList());
		int totalWidth = tableWidths.stream().mapToInt(i -> i).sum() 
			+ (qsBoxes.size() - 1) * 50; // 50px padding
		int maxHeight = tableHeights.stream().mapToInt(i -> i).max().orElse(0);

		ob.setSize(totalWidth,  50 + maxHeight);
		
		cb.setParentAndPosition(ob, 0, 0);
		cb.setSize(totalWidth,  60); 
		
		Box b = new Box();
		b.setParentAndPosition(cb,  0,  0);
		b.setLabel("UNION");
		b.setFill(new Color(179, 179, 179)); // #b3b3b3
		b.setSize(totalWidth, 30);
		int offset = 0;
		for (Box qsb : qsBoxes) {
			qsb.setParentAndPosition(ob, offset, 50);
			int w = qsb.getWidth();
			qsb.connectTo(cb, "sv"); // , offset + (w / 2), 30 // hrm. how does this work then
			offset += w + 20;
		}
		ob.setEdgeStartPosition(totalWidth / 2, 0);
		
		return ob;
	}
	
	private Box layout(AttachedSubqueriesNode n) {
		// @TODO multiple qsns
		List<QuerySpecificationNode> qsnList = Collections.singletonList(n.getQuerySpecification());
		
		Box ob = new CBox(); // outer box
		Box cb = new CBox(); // connector box
		
		List<Box> qsBoxes = reverseStream(qsnList)
			.map(c -> layout(c, "subquery"))
			.collect(Collectors.toList());
		
		List<Integer> tableWidths = qsBoxes.stream().map(b -> b.getWidth()).collect(Collectors.toList());
		List<Integer> tableHeights = qsBoxes.stream().map(b -> b.getHeight()).collect(Collectors.toList());
		int totalWidth = tableWidths.stream().mapToInt(i -> i).sum() 
			+ (qsBoxes.size() - 1) * 50; // 50px padding
		int maxHeight = tableHeights.stream().mapToInt(i -> i).max().orElse(0);

		ob.setSize(totalWidth,  30 + maxHeight);
		
		cb.setParentAndPosition(ob, 0, 0);
		cb.setSize(totalWidth,  30); 
		
		Box b = new Box();
		b.setParentAndPosition(cb,  0,  0);
		b.setLabel("attached_subqueries");
		// b.setFill(new Color(0, 0, 0)); // #b3b3b3
		b.setSize(totalWidth, 30);
		b.setStrokeDashArray(Arrays.asList(new String[] { "2" }));
		int offset = 0;
		for (Box qsb : qsBoxes) {
			qsb.setParentAndPosition(ob, offset, 30);
			int w = qsb.getWidth();
			qsb.connectTo(cb, "sv"); // , offset + (w / 2), 30 // hrm. how does this work then
			offset += w + 20;
		}
		ob.setEdgeStartPosition(0, 15);
		
		return ob;
	}
	
	
	private Box layout(DuplicatesRemovalNode n) {
		NestedLoopNode nln = n.getNestedLoop(); // 1 child only
		Box qb = layout(nln);
		
		int w = qb.getWidth();
		int h = qb.getHeight();
		
		Box ob = new CBox(); // outer box
		ob.setSize(w, h + 10 + 45);
		ob.setEdgeStartPosition(qb.getEdgeStartX(),  0);

		Box cb = new CBox(); // container box
		cb.setParentAndPosition(ob,  qb.getEdgeStartX() - 40, 0);
		cb.setSize(80, n.getUsingTemporaryTable() ? 55 : 40);
		
		Box lb = new Box(); // label box
		lb.setParentAndPosition(ob, qb.getEdgeStartX() - 40, 0);
		lb.setCssClass("duplicatesRemoval");
		lb.setLabel("DISTINCT"); 
		lb.setSize(80, 40);
		
		if (n.getUsingTemporaryTable()) {
			Box ttBox = new CBox(); 
			ttBox.setLabel("tmp table");
			ttBox.setSize(80, 10);
			ttBox.setCssClass("tempTableName");
			ttBox.setTextAnchor("start");
			ttBox.setParentAndPosition(ob, qb.getEdgeStartX() - 40, 45);
		}

		qb.connectTo(cb, "s"); // "up", 50, 30
		qb.setParentAndPosition(ob, 0, 40);
		
		return ob;

	}
	
	private Box layout(NestedLoopNode n) {
		List<Box> nestedLoopBoxes = new ArrayList<Box>();
		List<TableNode> qsnList = n.getTables();
		
		List<Box> tableBoxes = qsnList.stream() // reverseStream(qsnList)
			.map(c -> layout(c))
			.collect(Collectors.toList());
		List<Integer> tableWidths = tableBoxes.stream().map(b -> b.getWidth()).collect(Collectors.toList());
		List<Integer> tableHeights = tableBoxes.stream().map(b -> b.getHeight()).collect(Collectors.toList());
		
		int totalWidth = tableWidths.stream().mapToInt(i -> i).sum() 
			+ (tableBoxes.size() - 1) * 50; // 50px padding
		int maxHeight = tableHeights.stream().mapToInt(i -> i).max().orElse(0);

		Box ob = new CBox(); // outer box
		ob.setSize(totalWidth, 50 + 60 + 50 + maxHeight); // arrow + diamond + arrow + tables
		
		int offset = 0;
		for (int i = 0; i < tableBoxes.size(); i++) {
			Box tb = tableBoxes.get(i);
			tb.setParentAndPosition(ob, offset, 50 + 60 + 50);
			offset += tableWidths.get(i) + 50;
		}
		 
				
		Box prevNestedLoopBox = null;
		for (int i = 1 ; i < tableBoxes.size(); i++) {
			Box tb = tableBoxes.get(i);
			Box b = new Box(); 
			TableNode qsn = n.getTables().get(i);
			b.setShape("nestedLoop");
			b.setSize(60, 60); // diamond
			b.setParentAndPosition(ob, tb.getPosX() + tb.getEdgeStartX() - 30, 50); // centered above table beneath it
			b.setTooltip("nested_loop\n\n" +
			   "Prefix Cost: " + qsn.getCostInfo().getPrefixCost());
			nestedLoopBoxes.add(b);
			
			Box lb = new CBox(); // label box
			lb.setCssClass("queryCost");
			lb.setParentAndPosition(b, -10, -15);
			lb.setLabel(String.valueOf(qsn.getCostInfo().getPrefixCost())); 
			lb.setTextAnchor("start");
			lb.setSize(40, 10);

			if (i == tableBoxes.size() - 1) {
				lb = new CBox(); // label box
				lb.setCssClass("queryCost");
				lb.setParentAndPosition(b, 40, -10);
				lb.setLabel(String.valueOf(qsn.getRowsProducedPerJoin()) +
					(qsn.getRowsProducedPerJoin() == 1 ? " row" : " rows")); 
				lb.setTextAnchor("start");
				lb.setSize(25, 10);
			} else {
				lb = new CBox(); // label box
				lb.setCssClass("queryCost");
				lb.setParentAndPosition(b, 65, 15);
				lb.setLabel(String.valueOf(qsn.getRowsProducedPerJoin()) +
					(qsn.getRowsProducedPerJoin() == 1 ? " row" : " rows")); 
				lb.setTextAnchor("start");
				lb.setSize(25, 10);
			}

			
			if (i == 1) {
				Box firstTableBox = tableBoxes.get(0);
				firstTableBox.connectTo(b, "w"); // "upRight", 0, 15
				firstTableBox.setConnectedWeight((double) qsnList.get(0).getRowsExaminedPerScan());
			} else {
				prevNestedLoopBox.connectTo(b, "w"); // "right", 0, 15
				prevNestedLoopBox.setConnectedWeight((double) qsnList.get(i - 1).getRowsExaminedPerScan()); // @TODO not sure about this either
				prevNestedLoopBox.setEdgeStartPosition(60, 30);
			}
			
			Box tableBox = tableBoxes.get(i);
			tableBox.setConnectedWeight((double) qsnList.get(i).getRowsExaminedPerScan());
			tableBox.connectTo(b, "s"); // "up", 15, 30
			
			prevNestedLoopBox = b;
		}

		ob.setEdgeStartPosition(prevNestedLoopBox.getPosX() + 30, 50); // although the edge has already been drawn, so this is the edge end position really. maybe not. 
		return ob;
	}
			
	private Box layout(TableNode n) {
		
		int w = (n.getAccessType()==AccessTypeEnum.FULL_TABLE_SCAN ? 100 :
			(n.getAccessType()==AccessTypeEnum.FULL_INDEX_SCAN ? 100 :
			(n.getAccessType()==AccessTypeEnum.NON_UNIQUE_KEY ? 150 :
			(n.getAccessType()==AccessTypeEnum.UNIQUE_KEY ? 125 : 100)))); 
		int h = 60;
		Box ob = new CBox(); // outer box
		
		if (n.getMaterialisedFromSubquery() == null) {
			if (n.getTableName() != null) {
				Box lb = new CBox(); // label box
				lb.setCssClass("tableName");
				lb.setParentAndPosition(ob, 0, 32);
				lb.setLabel(n.getTableName()); 
				lb.setSize(w, 14);
			}
			if (n.getKey() != null) {
				Box lb = new CBox(); // label box
				lb.setCssClass("tableKey"); 
				lb.setParentAndPosition(ob, 0, 46);
				lb.setLabel(n.getKey()); 
				lb.setSize(w, 14);
			}
		} else {
			QueryBlockNode queryBlock = n.getMaterialisedFromSubquery().getQueryBlock();
			Box qb;
			qb = layout(queryBlock, null); // query_blocks in materialised queries aren't drawn for some reason
			// reset to 0,0

			RangeVisitor rv = new RangeVisitor();
			qb.traverse(rv);
			logger.info("materialised subquery range [" + rv.getMinX() + ", " + rv.getMinY() + "] - [" + rv.getMaxX() + ", " + rv.getMaxY() + "]");
			// qb.posX -= rv.getMinX();
			// qb.posY -= rv.getMinY();

			h = 80 + qb.getHeight() + 20; // 20px padding bottom
			w = Math.max(w, qb.getWidth() + 20); // 10px padding left and right

			Box lb = new CBox();
			lb.setCssClass("dotted"); // dotted line box
			lb.setStroke(new Color(140, 140, 140)); // #8c8c8c
			lb.setStrokeDashArray(Arrays.asList(new String[] { "4" }));
			lb.setParentAndPosition(ob, 0, 0);
			lb.setSize(w, h);
			
			// qb after lb in the diagram so that tooltips work
			qb.setParentAndPosition(ob, 10 - rv.getMinX(), 85);

			if (n.getTableName() != null) {
				lb = new CBox(); // label box
				lb.setCssClass("materialisedTableName");
				lb.setFill(new Color(232, 232, 232));
				lb.setParentAndPosition(ob, 0, 32);
				lb.setLabel(n.getTableName() + " (materialised)"); 
				lb.setSize(w, 20);
			}
			if (n.getKey() != null) {
				lb = new CBox(); // label box
				lb.setCssClass("tableKey"); 
				lb.setParentAndPosition(ob, 0, 52);
				lb.setLabel(n.getKey()); 
				lb.setSize(w, 14);
			}

		}

		ob.setEdgeStartPosition(w / 2, 0);
		
		Box b = new Box(); 
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
			double cost= (costInfo.getEvalCost() == null ? (double) 0 : costInfo.getEvalCost()) +
				(costInfo.getReadCost() == null ? (double) 0 : costInfo.getReadCost());
			Box lb = new CBox(); // label box
			lb.setCssClass("lhsQueryCost"); lb.setTextAnchor("start");
			lb.setParentAndPosition(ob, 0, -15);
			lb.setLabel(String.valueOf(cost)); 
			lb.setSize(w/2, 10);
		}
		if (n.getRowsExaminedPerScan() != null) {
			Box lb = new CBox(); // label box
			lb.setCssClass("rhsQueryCost");  lb.setTextAnchor("end");
			lb.setParentAndPosition(ob, w/2, -15);
			lb.setLabel(String.valueOf(n.getRowsExaminedPerScan()) + 
				(n.getRowsExaminedPerScan() == 1 ? " row" : " rows")); 
			lb.setSize(w/2, 10);
		}
		
		if (n.getAttachedSubqueries() != null) {
			Box qb = layout(n.getAttachedSubqueries());
			qb.setParentAndPosition(ob, w + 50, 0);
			qb.connectTo(b, "e");
			w = w + 50 + qb.getWidth();
			h = Math.max(h,  qb.getHeight());
		
		}
		
		ob.setSize(w, h);
		return ob;
		
	}
	private Box layout(OrderingOperationNode n) {

		NestedLoopNode nln = n.nestedLoop; // 1 child only
		Box cb = layout(nln);
		
		int w = cb.getWidth();
		int h = cb.getHeight();
		
		Box ob = new CBox(); // outer box, nestedLoop exit edge is drawn inside the nestedLoop box
		ob.setSize(w, h + 40);
		ob.setEdgeStartPosition(cb.getEdgeStartX(),  0);
		
		if (n.isUsingTemporaryTable()) { 
			Box tb = new CBox(); // label box
			tb.setParentAndPosition(ob, cb.getEdgeStartX() - 40, 45); 
			tb.setSize(w, 10);
			tb.setCssClass("tempTableName");
			tb.setLabel("tmp table"); 
		}
		
		Box lb = new Box(); // label box
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
	private Box layout(GroupingOperationNode n) {
		
		NestedLoopNode nln = n.getNestedLoop(); // 1 child only
		Box cb = layout(nln);
		
		int w = cb.getWidth();
		int h = cb.getHeight();
		
		Box ob = new CBox(); // outer box 
		ob.setSize(w, h + 40);
		ob.setEdgeStartPosition(cb.getEdgeStartX(),  0);
		
		Box lb = new Box(); // label box
		lb.setParentAndPosition(ob, cb.getEdgeStartX() - 40, 0);
		lb.setCssClass("groupingOperation");
		lb.setLabel("GROUP"); 
		lb.setSize(80, 40);

		cb.connectTo(lb, "s"); // "up", 50, 30
		cb.setParentAndPosition(ob, 0, 40);
		
		return ob;
		
		
	}
	
	private Box layout(QuerySpecificationNode n, String queryBlockLabel) {

		QueryBlockNode qb = n.getQueryBlock();
		
		Box cb = layout(qb, queryBlockLabel);
		
		int w = cb.getWidth();
		int h = cb.getHeight();
		
		Box ob = new CBox(); 
		ob.setSize(w, h + 40);
		ob.setEdgeStartPosition(cb.getEdgeStartX(),  40);

		cb.setParentAndPosition(ob, 0, 40);
		return ob;
	}
	
	private Box layout(Node n) {
		throw new UnsupportedOperationException("layout for node " + n.getClass().getName() + " not implemented");
	}
	
	protected <T> Stream<T> reverseStream(List<T> children) {
		// why the hell isn't this on https://stackoverflow.com/questions/24010109/java-8-stream-reverse-order 
		// ?
		return IntStream.range(0, children.size())
			.mapToObj(i -> children.get(children.size() - i - 1)); 
	}

	
}