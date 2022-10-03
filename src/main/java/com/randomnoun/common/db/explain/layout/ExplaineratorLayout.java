package com.randomnoun.common.db.explain.layout;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.log4j.Logger;

import com.randomnoun.common.Text;
import com.randomnoun.common.db.explain.enums.AccessTypeEnum;
import com.randomnoun.common.db.explain.graph.Shape;
import com.randomnoun.common.db.explain.json.AttachedSubqueriesNode;
import com.randomnoun.common.db.explain.json.BufferResultNode;
import com.randomnoun.common.db.explain.json.CostInfoNode;
import com.randomnoun.common.db.explain.json.DuplicatesRemovalNode;
import com.randomnoun.common.db.explain.json.GroupingOperationNode;
import com.randomnoun.common.db.explain.json.HavingSubqueriesNode;
import com.randomnoun.common.db.explain.json.NestedLoopNode;
import com.randomnoun.common.db.explain.json.Node;
import com.randomnoun.common.db.explain.json.OrderingOperationNode;
import com.randomnoun.common.db.explain.json.QueryBlockNode;
import com.randomnoun.common.db.explain.json.QuerySpecificationNode;
import com.randomnoun.common.db.explain.json.SharingTemporaryTableWithNode;
import com.randomnoun.common.db.explain.json.TableNode;
import com.randomnoun.common.db.explain.json.UnionResultNode;
import com.randomnoun.common.db.explain.json.WindowNode;
import com.randomnoun.common.db.explain.json.WindowingNode;
import com.randomnoun.common.db.explain.json.WindowsNode;
import com.randomnoun.common.db.explain.visitor.RangeShapeVisitor;

/** Converts a hierarchy of Nodes into a hierarchy of Shapes */
public class WorkbenchLayout implements Layout {

	Logger logger = Logger.getLogger(WorkbenchLayout.class);
	
	private QueryBlockNode topNode;
	protected boolean workbenchCompatible = true;
	
	public void setQueryBlockNode(QueryBlockNode topNode) {
		this.topNode = topNode;
	}

	public Shape getLayoutShape() {
		// probably need to reposition everything so that it starts at 0,0
		return layout(topNode, "query_block", true);
	}

	private <T> Stream<T> reverseStream(List<T> children) {
		// this is the sort of thing I would have expected to find on 
		//   https://stackoverflow.com/questions/24010109/java-8-stream-reverse-order 
		// but didn't
		return IntStream.range(0, children.size())
			.mapToObj(i -> children.get(children.size() - i - 1)); 
	}

	private String toSiUnits(Long v) {
		DecimalFormat df =  new DecimalFormat("0.##");
		if (v == null) { return "0"; }
		if (v < 1_000) { return String.valueOf(v); }
		else if (v < 1_000_000)                  { return df.format((double) v / 1_000) + "K"; }
		else if (v < 1_000_000_000)              { return df.format((double) v / 1_000_000) + "M"; }
		else if (v < 1_000_000_000_000L)         { return df.format((double) v / 1_000_000_000) + "G"; }
		else if (v < 1_000_000_000_000_000L)     { return df.format((double) v / 1_000_000_000_000L) + "T"; }
		else if (v < 1_000_000_000_000_000_000L) { return df.format((double) v / 1_000_000_000_000_000L) + "P"; }
		else { return String.valueOf(v); }
	}
	
	private List<String> escapeHtml(List<String> list) {
		if (list == null) { return Collections.emptyList(); }
		return list.stream().map(Text::escapeHtml).collect(Collectors.toList());
	}
		

	private Shape layout(QueryBlockNode n, String queryBlockLabel) {
		return layout(n, queryBlockLabel, false);
	}
	
	private Shape layout(QueryBlockNode n, String queryBlockLabel, boolean topNode) {
		Node c = n.getQueryNode(); // 1 child only
		
		Shape outer = new Shape(); // outer shape
		Shape child = null; // child shape
		Shape insertIntoShape = null;   // insert into shape
		Shape insertFromShape = null; // insert from shape
		Shape s = outer;
		int w = 100;
		int h = 0;
		int m = 0; // margin
		if (c != null) {
			child = layout(c);

			w = child.getWidth();
			h = child.getHeight();
		}
		
		if (n.getInsertFromNode() != null) {
			insertIntoShape = new Shape();
			insertIntoShape.setCssClass("insertQueryBlock");
			insertIntoShape.setParentAndPosition(outer, 0, 0);
			s = insertIntoShape;
			m = 10;
			h += m * 2;
			w += m * 2;
		}
		
		if (queryBlockLabel != null) {
			String labelText = queryBlockLabel + (topNode || n.getSelectId() == null ? "" : " #" + n.getSelectId()); // n.selectId == null || n.selectId == 1
			String clazz = (topNode ? " topNode" : ""); // n.selectId == null || n.selectId == 1
			String tooltip = "Select ID: " + n.getSelectId() + "\n" +
			  (n.getCostInfo() == null ? "" :"Query cost: " + n.getCostInfo().getQueryCost() + "\n");
			
			h += 30;
			Shape boxShape = new Shape(); // label shape
			boxShape.setCssClass("queryBlock" + clazz);
			if (child == null) {
				boxShape.setParentAndPosition(s, m, m);
				outer.setEdgeStartPosition(50, m);
			} else {
				boxShape.setParentAndPosition(s, child.getEdgeStartX() - 50 + m, m);
				outer.setEdgeStartPosition(child.getEdgeStartX() + m, m);
			}
			boxShape.setSize(100, 30);
			boxShape.setLabel(labelText); 
			boxShape.setTooltip(tooltip);
			
			CostInfoNode costInfo = n.getCostInfo();
			if (costInfo != null) {
				DecimalFormat df = new DecimalFormat("0.00");
				double cost = (costInfo.getQueryCost() == null ? (double) 0 : costInfo.getQueryCost());
				Shape costLabel = new Shape(); // label shape
				costLabel.setCssClass("lhsQueryCost"); costLabel.setTextAnchor("start");
				costLabel.setParentAndPosition(boxShape, 0, -15);
				costLabel.setLabel("Query cost: " + df.format(cost)); 
				costLabel.setSize(w/2, 10);
			}

			
			if (child == null) {
				Shape noTableShape = new Shape(); // label shape
				noTableShape.setCssClass("tableName" + clazz);
				noTableShape.setParentAndPosition(s, m, 35 + m);
				noTableShape.setSize(100, 10);
				noTableShape.setLabel("No tables used"); 
				noTableShape.setTooltip(tooltip);
				
			} else {
				h += 50; // 30 for the arrow and 20 bottom padding
				child.connectTo(boxShape, "s");
				child.setParentAndPosition(s, m, 60 + m);
			}
		} else if (child != null) {
			child.setParentAndPosition(outer, m, m);
		} else {
			throw new IllegalStateException("drawQueryBlock = false and no child block present");
		}
		
		if (n.getInsertFromNode() != null) {
			insertIntoShape = new Shape();
			insertIntoShape.setCssClass("insertQueryBlock");
			insertIntoShape.setParentAndPosition(outer, 0, 0);
			// outer.setEdgeStartPosition(50, 0);
			
			insertFromShape = layout(n.getInsertFromNode().getQueryNode());
			insertFromShape.connectTo(insertIntoShape, "sv"); // not really the outer any more
			insertFromShape.setParentAndPosition(outer, 0, h + 30);
			
			w = Math.max(w, insertFromShape.getWidth());
			insertIntoShape.setSize(w, h);
			
			h += 50 + insertFromShape.getHeight();
		}
		
		outer.setSize(w, h);
		return outer;
	}
	
	private Shape layout(WindowingNode n) {
		WindowsNode windowsNode = n.getWindows();
		List<WindowNode> windowList = windowsNode.getWindows();
		Node cn = n.getChildNode();
		
		Shape outer = new Shape();
		
		List<Shape> windowShapes = windowList.stream()
			.map(c -> layout(c))
			.collect(Collectors.toList());
			
		List<Integer> windowWidths = windowShapes.stream().map(Shape::getWidth).collect(Collectors.toList());
		List<Integer> windowHeights = windowShapes.stream().map(Shape::getHeight).collect(Collectors.toList());
		int totalWindowWidth = windowWidths.stream().mapToInt(i -> i).sum() 
			+ (windowShapes.size() - 1) * 20 + 20; // 20px padding between, 10px outer margin both side
		
		int h = windowHeights.stream().mapToInt(i -> i).max().orElse(0) + 30 + 20 ; // 10px outer margin top and bottom + label + windows
		
		Shape windowingBorder = new Shape(); // outer shape
		windowingBorder.setParentAndPosition(outer, 0, 0);
		windowingBorder.setCssClass("windowingBorder");
		windowingBorder.setSize(totalWindowWidth, h);
		windowingBorder.setEdgeStartPosition(totalWindowWidth / 2, 0);
		
		int offset = 10;
		for (Shape ws : windowShapes) {
			ws.setParentAndPosition(windowingBorder, offset, 10);
			// qsb.connectTo(connector, "sv"); // , offset + (w / 2), 30 // hrm. how does this work then
			offset += ws.getWidth() + 20;
		}
		
		Shape windowingLabel = new Shape();
		windowingLabel.setCssClass("windowing");
		windowingLabel.setParentAndPosition(windowingBorder,  0,  h - 30);
		windowingLabel.setLabel("windowing");
		// union.setFill(new Color(179, 179, 179)); // #b3b3b3
		windowingLabel.setSize(totalWindowWidth, 30);
		
		Shape child = layout(cn);
		child.connectTo(windowingBorder, "sv"); // not really the outer any more
		child.setParentAndPosition(windowingBorder, totalWindowWidth / 2 - child.getEdgeStartX(), 90 + 50);
		
		// child.setParentAndPosition(outer, /*(totalWidth - child.getWidth()) / 2*/ child.getEdgeStartX() / 2, 90 + 50);
		int outerWidth = Math.max(totalWindowWidth, child.getWidth());
		int outerHeight = h + child.getHeight() + 90 + 50;
		
		outer.setSize(outerWidth, outerHeight);
		outer.setEdgeStartPosition(windowingBorder.getEdgeStartX(), windowingBorder.getEdgeStartY());
		return outer;
	}
	
	private Shape layout(WindowNode n) {
		Shape outer;
		Shape boxShape;

		outer = new Shape();
		boxShape = new Shape(); 
		boxShape.setCssClass("window" + (n.isUsingTemporaryTable() ? " hasTempTable" : ""));
		boxShape.setLabel("window"); // <unnamed window>  
		boxShape.setSize(120, 40);
		boxShape.setTooltip("<span class=\"windowHeader\">Window</span>\n\n" +
		    (n.isUsingTemporaryTable() ? "Using Temporary Table: true\n" : "") +
			"Definition position: " + n.getDefinitionPosition() + "\n\n" +
		    "Functions:\n  " + Text.join(n.getFunctions(), ", "));
		
		Shape labelShape = new Shape(); // label underneath the box
		// String label = n.isUsingTemporaryTable() ? "tmp table" : "";
		// label += n.isUsingFilesort() ? (label.equals("") ? "" : ", ") + "filesort" : "";
		labelShape.setParentAndPosition(boxShape, 0, 45); 
		labelShape.setSize(120, 10);
		labelShape.setCssClass("windowName");
		labelShape.setLabel(n.getName()); 
		
		outer.setSize(120, 55); // w + 160
		// outer.setEdgeStartPosition(newEdgeStartX + 100, 0);
		boxShape.setParentAndPosition(outer, 0, 0);
		return outer;
		
	}
	
	private Shape layout(UnionResultNode n) {
		List<QuerySpecificationNode> qsnList = n.getQuerySpecifications();
		
		Shape outer = new Shape(); // outer shape
		Shape connector = new Shape(); // connector shape
		
		List<Shape> qsShapes = reverseStream(qsnList)
			.map(c -> layout(c, "query_block"))
			.collect(Collectors.toList());
		
		List<Integer> tableWidths = qsShapes.stream().map(Shape::getWidth).collect(Collectors.toList());
		List<Integer> tableHeights = qsShapes.stream().map(Shape::getHeight).collect(Collectors.toList());
		int totalWidth = tableWidths.stream().mapToInt(i -> i).sum() 
			+ (qsShapes.size() - 1) * 50; // 50px padding
		int maxHeight = tableHeights.stream().mapToInt(i -> i).max().orElse(0);

		outer.setSize(totalWidth,  50 + maxHeight);
		
		connector.setParentAndPosition(outer, 0, 0);
		connector.setSize(totalWidth,  60); 
		
		Shape union = new Shape();
		union.setCssClass("union");
		union.setParentAndPosition(connector,  0,  0);
		union.setLabel("UNION");
		// union.setFill(new Color(179, 179, 179)); // #b3b3b3
		union.setSize(totalWidth, 30);
		int offset = 0;
		for (Shape qsb : qsShapes) {
			qsb.setParentAndPosition(outer, offset, 50);
			int w = qsb.getWidth();
			qsb.connectTo(connector, "sv"); // , offset + (w / 2), 30 // hrm. how does this work then
			offset += w + 20;
		}
		outer.setEdgeStartPosition(totalWidth / 2, 0);
		
		return outer;
	}
	
	private Shape layout(AttachedSubqueriesNode n) {
		List<QuerySpecificationNode> qsnList = n.getQuerySpecifications();
		
		Shape outer = new Shape(); // outer shape
		Shape connector = new Shape(); // connector shape
		
		List<Shape> qsShapes = reverseStream(qsnList)
			.map(c -> layout(c, "subquery"))
			.collect(Collectors.toList());
		
		List<Integer> tableWidths = qsShapes.stream().map(Shape::getWidth).collect(Collectors.toList());
		List<Integer> tableHeights = qsShapes.stream().map(Shape::getHeight).collect(Collectors.toList());
		int totalWidth = tableWidths.stream().mapToInt(i -> i).sum() 
			+ (qsShapes.size() - 1) * 50; // 50px padding
		int maxHeight = tableHeights.stream().mapToInt(i -> i).max().orElse(0);

		int w = Math.max(totalWidth, 150);
		
		outer.setSize(w,  50 + maxHeight);
		
		connector.setParentAndPosition(outer, 0, 0);
		connector.setSize(w,  30); 
		
		Shape attachedSubqueries = new Shape();
		attachedSubqueries.setCssClass("attachedSubqueries");
		attachedSubqueries.setParentAndPosition(connector,  0,  0);
		attachedSubqueries.setLabel("attached_subqueries");
		attachedSubqueries.setSize(w, 30);
		int offset = (w - totalWidth) / 2;
		for (Shape qsShape : qsShapes) {
			qsShape.setParentAndPosition(outer, offset, 50);
			int sw = qsShape.getWidth();
			qsShape.connectTo(connector, "sv"); // south port, vertical line
			offset += sw + 50;
		}
		outer.setEdgeStartPosition(0, 15);
		
		return outer;
	}
	
	private Shape layout(HavingSubqueriesNode n) {
		List<QuerySpecificationNode> qsnList = n.getQuerySpecifications();
		
		Shape outer = new Shape(); // outer shape
		Shape connector = new Shape(); // connector shape
		
		List<Shape> qsShapes = reverseStream(qsnList)
			.map(c -> layout(c, "subquery"))
			.collect(Collectors.toList());
		
		List<Integer> tableWidths = qsShapes.stream().map(Shape::getWidth).collect(Collectors.toList());
		List<Integer> tableHeights = qsShapes.stream().map(Shape::getHeight).collect(Collectors.toList());
		int totalWidth = tableWidths.stream().mapToInt(i -> i).sum() 
			+ (qsShapes.size() - 1) * 50; // 50px padding
		int maxHeight = tableHeights.stream().mapToInt(i -> i).max().orElse(0);

		int w = Math.max(totalWidth, 150);
		
		outer.setSize(w,  50 + maxHeight);
		
		connector.setParentAndPosition(outer, 0, 0);
		connector.setSize(w,  30); 
		
		Shape havingSubqueries = new Shape();
		havingSubqueries.setCssClass("havingSubqueries");
		havingSubqueries.setParentAndPosition(connector,  0,  0);
		havingSubqueries.setLabel("having_subqueries");
		havingSubqueries.setSize(w, 30);
		int offset = (w - totalWidth) / 2;
		for (Shape qsShape : qsShapes) {
			qsShape.setParentAndPosition(outer, offset, 50);
			int sw = qsShape.getWidth();
			qsShape.connectTo(connector, "sv"); // south port, vertical line
			offset += sw + 50;
		}
		outer.setEdgeStartPosition(0, 15);
		
		return outer;
	}
	
	
	private Shape layout(NestedLoopNode n) {
		List<Shape> nestedLoopShapes = new ArrayList<>();
		List<TableNode> qsnList = n.getTables();
		
		/* distances:
		 * 
		 *                          ^
		 *                          : topArrow
		 *                          ^
		 *    ___________________  / \ 
		 *    :                    \ /
		 *    :                     : bottomArrow
		 * [     ] -tableMargin- [     ]
		 */
		List<Shape> tableShapes = qsnList.stream() // reverseStream(qsnList)
			.map(this::layout)
			.collect(Collectors.toList());
		List<Integer> tableWidths = tableShapes.stream().map(Shape::getWidth).collect(Collectors.toList());
		List<Integer> tableHeights = tableShapes.stream().map(Shape::getHeight).collect(Collectors.toList());
		
		int totalWidth = tableWidths.stream().mapToInt(i -> i).sum() 
			+ (tableShapes.size() - 1) * 50; // 50px padding
		int maxHeight = tableHeights.stream().mapToInt(i -> i).max().orElse(0);
		
		// should get these from the css
		int topArrow = 0;
		int bottomArrow = 50;
		int tableMargin = 50;
		int diamondWidth = 60;
		
		Shape outer = new Shape(); // outer shape
		outer.setSize(totalWidth, topArrow + diamondWidth + bottomArrow + maxHeight); // arrow + diamond + arrow + tables
		
		int offset = 0;
		for (int i = 0; i < tableShapes.size(); i++) {
			Shape tableShape = tableShapes.get(i);
			tableShape.setParentAndPosition(outer, offset, topArrow + diamondWidth + bottomArrow);
			offset += tableWidths.get(i) + tableMargin;
		}
				
		Shape prevNestedLoopShape = null;
		Shape lastTableShape = null;
		TableNode qsn = null;
		for (int i = 1 ; i < tableShapes.size(); i++) {
			Shape tabelShape = tableShapes.get(i);
			Shape diamond = new Shape(); 
			qsn = n.getTables().get(i);
			diamond.setShape("nestedLoop");
			diamond.setSize(diamondWidth, diamondWidth); // diamond
			diamond.setParentAndPosition(outer, tabelShape.getPosX() + tabelShape.getEdgeStartX() - diamondWidth / 2, topArrow); // centered above table beneath it
			diamond.setTooltip("<span class=\"nestedLoopHeader\">nested_loop</span>\n\n" +
			   (qsn.getCostInfo() == null ? "" : "Prefix Cost: " + qsn.getCostInfo().getPrefixCost()));
			nestedLoopShapes.add(diamond);
			
			Shape costShape;
			if (qsn.getCostInfo() != null) {
				costShape = new Shape(); // label shape
				costShape.setCssClass("queryCost");
				costShape.setParentAndPosition(diamond, -10, -15);
				costShape.setLabel(String.valueOf(qsn.getCostInfo().getPrefixCost())); 
				costShape.setTextAnchor("start");
				costShape.setSize(40, 10);
			}

			if (i == tableShapes.size() - 1) {
				costShape = new Shape(); // label shape
				costShape.setCssClass("queryCost");
				costShape.setParentAndPosition(diamond, diamondWidth / 2 + 10, -10);
				costShape.setLabel(qsn == null || qsn.getRowsProducedPerJoin() == null ? "0 rows" :
					(toSiUnits(qsn.getRowsProducedPerJoin()) + (qsn.getRowsProducedPerJoin() == 1 ? " row" : " rows"))); 
				costShape.setTextAnchor("start");
				costShape.setSize(25, 10);
			} else {
				costShape = new Shape(); // label shape
				costShape.setCssClass("queryCost");
				costShape.setParentAndPosition(diamond, diamondWidth + 5, 15);
				costShape.setLabel(qsn == null || qsn.getRowsProducedPerJoin() == null ? "0 rows" :
					(toSiUnits(qsn.getRowsProducedPerJoin()) + (qsn.getRowsProducedPerJoin() == 1 ? " row" : " rows"))); 
				costShape.setTextAnchor("start");
				costShape.setSize(25, 10);
			}

			
			if (i == 1) {
				Shape firstTableShape = tableShapes.get(0);
				firstTableShape.connectTo(diamond, "w"); // "upRight", 0, 15
				firstTableShape.setConnectedWeight((double) qsnList.get(0).getRowsExaminedPerScan());
			} else {
				prevNestedLoopShape.connectTo(diamond, "w"); // "right", 0, 15
				prevNestedLoopShape.setConnectedWeight((double) qsnList.get(i - 1).getRowsExaminedPerScan()); // @TODO not sure about this either
				prevNestedLoopShape.setEdgeStartPosition(60, 30);
			}
			
			lastTableShape = tableShapes.get(i);
			lastTableShape.setConnectedWeight((double) qsnList.get(i).getRowsExaminedPerScan());
			lastTableShape.connectTo(diamond, "s");
			prevNestedLoopShape = diamond;
		}

		if (qsn != null && qsn.getRowsProducedPerJoin() != null) { 
			outer.setConnectedWeight((double) qsn.getRowsProducedPerJoin()); 
		}
		outer.setEdgeStartPosition(prevNestedLoopShape.getPosX() + diamondWidth / 2, topArrow); // although the edge has already been drawn, so this is the edge end position really. maybe not. 
		return outer;
	}
			
	private Shape layout(TableNode n) {
		int w;
		switch (n.getAccessType()) {
			case FULL_TABLE_SCAN: w = 100; break;
			case FULL_INDEX_SCAN: w = 100; break;
			case NON_UNIQUE_KEY: w = 150; break;
			case UNIQUE_KEY: w = 125; break;
			default: w = 100;
		}
		if (n.isInsert()) { w += 50; }
		int lbsh = n.getAccessType()==AccessTypeEnum.CONST ? 45 : 30; // label box shape height
		int h = lbsh;
		Shape outer = new Shape(); // outer shape
		
		if (n.getMaterialisedFromSubquery() == null) {
			h += 30;
			if (n.getTableName() != null) {
				Shape label = new Shape(); // label shape
				label.setCssClass("tableName");
				label.setParentAndPosition(outer, 0, lbsh + 2);
				label.setLabel(n.getTableName()); 
				label.setSize(w, 14);
			}
			if (n.getKey() != null) {
				Shape label = new Shape(); // label shape
				label.setCssClass("tableKey"); 
				label.setParentAndPosition(outer, 0, lbsh + 16);
				label.setLabel(n.getKey()); 
				label.setSize(w, 14);
			}
		} else {
			Node subquery = n.getMaterialisedFromSubquery().getSubquery();
			if (subquery instanceof SharingTemporaryTableWithNode) {
				// @XXX this isn't from workbench
				SharingTemporaryTableWithNode sttw = (SharingTemporaryTableWithNode) subquery;
				/*
				Shape box = new Shape();
				box.setCssClass("materialisedFromSubqueryBorder"); // dotted line Shape
				box.setParentAndPosition(outer, 0, 42);
				box.setSize(w, h);
				*/ 
				
				Shape label = new Shape(); // label shape
				label.setCssClass("materialisedTableName");
				label.setParentAndPosition(outer, 0, 32);
				label.setLabel(n.getTableName() + " (shared with #" + sttw.getSelectId() + ")"); 
				label.setSize(w, 20);
				
			} else if (subquery instanceof QueryBlockNode) {
				QueryBlockNode queryBlock = (QueryBlockNode) subquery;
			
				Shape qbShape = layout(queryBlock, null); // query_blocks in materialised queries aren't drawn for some reason
				// reset to 0,0
	
				RangeShapeVisitor rv = new RangeShapeVisitor();
				qbShape.traverse(rv);
				logger.info("materialised subquery range [" + rv.getMinX() + ", " + rv.getMinY() + "] - [" + rv.getMaxX() + ", " + rv.getMaxY() + "]");
	
				h = 30 + 80 + qbShape.getHeight() + 20; // 20px padding bottom
				w = Math.max(w, qbShape.getWidth() + 20); // 10px padding left and right
				w = Math.max(w, 300);
	
				Shape box = new Shape();
				box.setCssClass("materialisedFromSubqueryBorder"); // dotted line Shape
				box.setParentAndPosition(outer, 0, 0);
				box.setSize(w, h);
				
				// qb after lb in the diagram so that tooltips work
				qbShape.setParentAndPosition(outer, /*10 - rv.getMinX()*/ (w - qbShape.getWidth()) / 2 - rv.getMinX(), 85);
	
				if (n.getTableName() != null) {
					Shape label = new Shape(); // label shape
					label.setCssClass("materialisedTableName");
					label.setParentAndPosition(outer, 0, 32);
					label.setLabel(n.getTableName() + " (materialised)"); 
					label.setSize(w, 20);
				}
				if (n.getKey() != null) {
					Shape label = new Shape(); // label shape
					label.setCssClass("tableKey"); 
					label.setParentAndPosition(outer, 0, 52);
					label.setLabel(n.getKey()); 
					label.setSize(w, 14);
				}
			} else {
				throw new IllegalStateException("unexpected subquery node " + n.getMaterialisedFromSubquery().getSubquery().getClass() +
					"; expected SharingTemporaryTableWithNode or QueryBlockNode");
			}

		}

		outer.setEdgeStartPosition(w / 2, 0);
		
		Shape labelBoxShape = new Shape(); 
		labelBoxShape.setParentAndPosition(outer, 0, 0);
		labelBoxShape.setSize(w, lbsh);
		labelBoxShape.setCssClass("table " + n.getAccessType().getCssClass() + (n.isInsert() ? " insert" : ""));
		labelBoxShape.setLabel(n.getAccessType().getLabel() + (n.isInsert() ? " (Insert)" : ""));

		DecimalFormat df = new DecimalFormat("0.##");
		String tooltip = 
			"<span class=\"tableHeader\">" + n.getTableName() + "</span>\n"  +
		    "  Access Type: " + n.getAccessType().getJsonValue() + "\n" +
			"    " + labelBoxShape.getLabel() + "\n" +
		    "    Cost Hint: " + n.getAccessType().getCostHint() + "\n" +
			"  Used Columns: " + Text.join(escapeHtml(n.getUsedColumns()), ",\n    ") + "\n" +
		    "\n" +
			"<span class=\"keyIndexHeader\">Key/Index: " + Text.escapeHtml(n.getKey()) + "</span>\n" +
		    (n.getRefs() == null ? "" : "  Ref.:" + Text.join(escapeHtml(n.getRefs()), ",\n    ") + "\n") +
		    "  Used Key Parts: " + Text.join(escapeHtml(n.getUsedKeyParts()), ",\n    ") + "\n" +
			"  Possible Keys: " + Text.join(escapeHtml(n.getPossibleKeys()), ",\n    ") + "\n" +
		    "\n" +
			(n.getAttachedCondition() == null ? "" : 
				"<span class=\"attachedConditionHeader\">Attached condition:</span>\n" +
				"  " + Text.escapeHtml(n.getAttachedCondition()) + "\n\n" ) +
			"Rows Examined per Scan: " + n.getRowsExaminedPerScan() + "\n" +
		    "Rows Produced per Join: " + n.getRowsProducedPerJoin() + "\n" +
			(n.getFiltered() == null ? "" : 
			"Filtered (ratio of rows produced per rows examined): " + df.format(n.getFiltered()) + "%\n" +
		    "  Hint: 100% is best, &lt;= 1% is worst\n" +
			"  A low value means the query examines a lot of rows that are not returned.\n") +
			(n.getCostInfo() == null ? "" : 
		    "<span class=\"costInfoHeader\">Cost Info</span>\n" +
			"  Read: " + df.format(n.getCostInfo().getReadCost()) + "\n" +
			"  Eval: " + df.format(n.getCostInfo().getEvalCost()) + "\n" +
			"  Prefix: " + df.format(n.getCostInfo().getPrefixCost()) + "\n" +
			"  Data Read: " + n.getCostInfo().getDataReadPerJoin());
		labelBoxShape.setTooltip(tooltip);
		
		CostInfoNode costInfo = n.getCostInfo();
		if (costInfo != null) {
			double cost = (costInfo.getEvalCost() == null ? (double) 0 : costInfo.getEvalCost()) +
				(costInfo.getReadCost() == null ? (double) 0 : costInfo.getReadCost());
			Shape costLabel = new Shape(); // label shape
			costLabel.setCssClass("lhsQueryCost"); costLabel.setTextAnchor("start");
			costLabel.setParentAndPosition(outer, 0, -15);
			costLabel.setLabel(df.format(cost)); 
			costLabel.setSize(w/2, 10);
		}
		if (n.getRowsExaminedPerScan() != null) {
			Shape costLabel = new Shape(); // label shape
			costLabel.setCssClass("rhsQueryCost");  costLabel.setTextAnchor("end");
			costLabel.setParentAndPosition(outer, w/2, -15);
			costLabel.setLabel(toSiUnits(n.getRowsExaminedPerScan()) + 
				(n.getRowsExaminedPerScan() == 1 ? " row" : " rows")); 
			costLabel.setSize(w/2, 10);
		}

		// @TODO attached on the lhs, having on the rhs
		if (n.getAttachedSubqueries() != null) {
			Shape qb = layout(n.getAttachedSubqueries());
			qb.setParentAndPosition(outer, w + 50, 0);
			qb.connectTo(labelBoxShape, "e");
			w = w + 50 + qb.getWidth();
			h = Math.max(h, qb.getHeight());
		}

		outer.setSize(w, h);
		return outer;
		
	}

	private Shape layout(OrderingOperationNode n) {
		Node cn = n.getOrderedNode();
		
		// for reasons I'm not entirely sure of, if cn is a DuplicatesRemovalNode ('DISTINCT'), 
		// then it's on the lhs of the ORDER shape, otherwise it's underneath
		
		Shape child = layout(cn);
		int w = child.getWidth();
		int h = child.getHeight();
		
		Shape outer;
		Shape boxShape;

		// ordering operations that create temp tables are coloured red
		// but distinct operations that create temp tables aren't. go figure.
		
		boxShape = new Shape(); 
		boxShape.setCssClass("orderingOperation" + (n.isUsingTemporaryTable() ? " hasTempTable" : ""));
		boxShape.setLabel("ORDER"); 
		boxShape.setSize(80, 40);
		boxShape.setTooltip("<span class=\"orderingOperationHeader\">Ordering operation</span>\n\n" +
		    (n.isUsingTemporaryTable() ? "Using Temporary Table: true\n" : "") +
			"Using Filesort: " + n.isUsingFilesort());

		if (n.isUsingTemporaryTable() || n.isUsingFilesort()) { 
			Shape labelShape = new Shape(); // label underneath the box
			String label = n.isUsingTemporaryTable() ? "tmp table" : "";
			label += n.isUsingFilesort() ? (label.equals("") ? "" : ", ") + "filesort" : "";
			labelShape.setParentAndPosition(boxShape, 0, 45); 
			labelShape.setSize(80, 10);
			labelShape.setCssClass("tempTableName");
			labelShape.setLabel(label); 
		}
		
		outer = new Shape(); 
		if (cn instanceof DuplicatesRemovalNode || cn instanceof GroupingOperationNode) {
			// child is left of this node
			int prevEdgeStartX = child.getEdgeStartX(); // edge would have started in middle of the 'DISTINCT' node
			int newEdgeStartX = prevEdgeStartX + 40;    // now it starts on the right hand side of it
			
			child.setEdgeStartX(newEdgeStartX);    
			child.setEdgeStartY(20);
			outer.setSize(newEdgeStartX + 160, h); // w + 160
			outer.setEdgeStartPosition(newEdgeStartX + 100, 0);

			boxShape.setParentAndPosition(outer, newEdgeStartX + 60, 0);

			child.connectTo(boxShape, "w");
			child.setParentAndPosition(outer, 0, 0);
			
		} else {
			// child is under this node
			outer.setSize(w, h + 90);
			outer.setEdgeStartPosition(child.getEdgeStartX(),  0);
			
			boxShape.setParentAndPosition(outer, child.getEdgeStartX() - 40, 0);

			child.connectTo(boxShape, "s"); // "up", 50, 30
			child.setParentAndPosition(outer, 0, 90);
		}

		
		outer.setConnectedWeight(child.getConnectedWeight());
		return outer;
	}
	
	private Shape layout(DuplicatesRemovalNode n) {
		Node cn = n.getChildNode(); // 1 child only
		
		Shape child = layout(cn);
		int w = child.getWidth();
		int h = child.getHeight();
		
		Shape boxShape = new Shape(); // box shape
		Shape container = new Shape(); // container shape ( line connects to container, not box )
		Shape labelShape = null; 
		boxShape.setCssClass("duplicatesRemoval");
		boxShape.setLabel("DISTINCT"); 
		boxShape.setSize(80, 40);
		boxShape.setTooltip("Duplicates removal\n\n" +
			"Using Temporary Table: " + n.isUsingTemporaryTable() + "\n" +
			"Using Filesort: " + n.isUsingFilesort());
		container.setSize(80, 40);
		
		if (n.isUsingTemporaryTable()) {
			labelShape = new Shape();
			labelShape.setLabel("tmp table");
			labelShape.setSize(80, 10);
			labelShape.setCssClass("tempTableName");
			labelShape.setTextAnchor("start");
			labelShape.setParentAndPosition(boxShape, 0, 45); // child.getEdgeStartX() - 40
			container.setSize(80, 55);
		}

		Shape outer = new Shape(); // outer shape
		if (cn instanceof GroupingOperationNode) {
			// child is left of this node
			int prevEdgeStartX = child.getEdgeStartX(); // edge would have started in middle of the 'GROUP' node
			int newEdgeStartX = prevEdgeStartX + 40;    // now it starts on the right hand side of it
			child.setEdgeStartX(newEdgeStartX);    
			child.setEdgeStartY(20);

			outer.setSize(newEdgeStartX + 160, h); // w + 160
			outer.setEdgeStartPosition(newEdgeStartX + 100, 0);

			boxShape.setParentAndPosition(outer, newEdgeStartX + 60, 0);
			container.setParentAndPosition(outer, newEdgeStartX + 60, 0);

			child.connectTo(boxShape, "w"); // connects to box, not container
			child.setParentAndPosition(outer, 0, 0);
			
		} else {
			// child is underneath this node
			outer.setSize(w, 40 + (n.isUsingTemporaryTable() ? 15 : 0) + 45 + h);
			outer.setEdgeStartPosition(child.getEdgeStartX(),  0);

			boxShape.setParentAndPosition(outer, child.getEdgeStartX() - 40, 0);
			container.setParentAndPosition(outer, child.getEdgeStartX() - 40, 0);

			child.connectTo(container, "s"); 
			child.setParentAndPosition(outer, 0, 40 + (n.isUsingTemporaryTable() ? 15 : 0) + 45 );

		}
		
		return outer;

	}
	
	private Shape layout(GroupingOperationNode n) {
		Node nln = n.getGroupedNode(); // 1 child only
		
		Shape child = layout(nln);
		int w = child.getWidth();
		int h = child.getHeight() + 90;
		
		Shape outer = new Shape(); // outer shape 
		outer.setEdgeStartPosition(child.getEdgeStartX(),  0);
		
		Shape boxShape = new Shape();
		boxShape.setCssClass("groupingOperation" + (n.isUsingTemporaryTable() ? " hasTempTable" : ""));
		boxShape.setLabel("GROUP"); 
		boxShape.setSize(80, 40);
		boxShape.setTooltip("<span class=\"groupingOperationHeader\">Grouping operation</span>\n\n" +
		    (n.isUsingTemporaryTable() ? "Using Temporary Table: true\n" : "") +
			"Using Filesort: " + n.isUsingFilesort());
		boxShape.setParentAndPosition(outer, child.getEdgeStartX() - 40, 0);
		
		if (n.getHavingSubqueries() != null) {
			Shape qb = layout(n.getHavingSubqueries());
			qb.setParentAndPosition(outer, w + 50, 5);
			qb.connectTo(boxShape, "e");
			w = w + 50 + qb.getWidth();
			h = Math.max(h, qb.getHeight());
		}

		outer.setSize(w, h + 90);		
		
		child.connectTo(boxShape, "s"); // "up", 50, 30
		child.setParentAndPosition(outer, 0, 90);
		
		outer.setConnectedWeight(child.getConnectedWeight());
		return outer;
		
		
	}
	
	private Shape layout(QuerySpecificationNode n, String queryBlockLabel) {

		QueryBlockNode qb = n.getQueryBlock();
		
		Shape child = layout(qb, queryBlockLabel);
		
		int w = child.getWidth();
		int h = child.getHeight();
		
		Shape outer = new Shape(); 
		outer.setSize(w, h + 40);
		outer.setEdgeStartPosition(child.getEdgeStartX(),  40);

		child.setParentAndPosition(outer, 0, 40);
		return outer;
	}
	
	// ignore the buffer result container, just layout the queryBlock within
	private Shape layout(BufferResultNode brn) {
		QueryBlockNode qbn = brn.getQueryBlock();
		return layout(qbn.getQueryNode());
	}

	// call this if there is a choice of different Node types, which will then call a more strongly typed layout method
	private Shape layout(Node node) {
		Shape shape;
		if (node instanceof UnionResultNode) { shape = layout((UnionResultNode) node); }
		else if (node instanceof DuplicatesRemovalNode) { shape = layout((DuplicatesRemovalNode) node); }
		else if (node instanceof TableNode) { shape = layout((TableNode) node); }
		else if (node instanceof OrderingOperationNode) { shape = layout((OrderingOperationNode) node); }
		else if (node instanceof GroupingOperationNode) { shape = layout((GroupingOperationNode) node); }
		else if (node instanceof NestedLoopNode) { shape = layout((NestedLoopNode) node); }
		else if (node instanceof WindowingNode) { shape = layout((WindowingNode) node); }
		else if (node instanceof BufferResultNode) { shape = layout((BufferResultNode) node); }
		else {
			throw new IllegalStateException("unexpected class " + node.getClass().getName() + " in layout()");
		}
		return shape;
	}
	
	
}