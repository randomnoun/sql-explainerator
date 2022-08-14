package com.randomnoun.common.db.explain;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.randomnoun.common.StreamUtil;
import com.randomnoun.common.Struct;
import com.randomnoun.common.Text;
import com.randomnoun.common.log4j.Log4jCliConfiguration;

/* Based on the explain renderer in MySQL Workbench, although it turns out that that code is GPLed
 * so I'll have to clean-room this instead.
 * 
 * TODO: run a bunch of query plans and see what kinds of images it produces.
 * 
 * ok so I think I'm going to have to use SVG at some stage so let's do that
 * also, roundtrip all the examples at https://github.com/joelsotelods/sakila-db-queries
 * since I should be able to bundle all those into a unit test without tripping over any licensing issues 
 * 
 */

public class ExplainerSvg {

	static Logger logger = Logger.getLogger(ExplainerSvg.class);
	
	private static enum AccessTypeEnum {
		FULL_TABLE_SCAN("ALL"), // red
		FULL_INDEX_SCAN("index"), // red
		NON_UNIQUE_KEY("ref"), // green
		UNIQUE_KEY("eq_ref"); // green
		private String jsonValue;
		private AccessTypeEnum(String jsonValue) {
			this.jsonValue = jsonValue;
		}
		public static AccessTypeEnum fromJsonValue(String jsonValue) {
			for (AccessTypeEnum e : AccessTypeEnum.values()) {
				if (e.jsonValue.equals(jsonValue)) { return e; }
			}
			throw new IllegalArgumentException("no AccessTypeEnum with jsonValue '" + jsonValue + "'");
		}
	}

	private static class NameList extends ArrayList<String> implements Struct.ToJson {

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
	
	private static class CostInfo implements Struct.ToJson {
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
	}
	
	// a Node represents an object or array in the mysql-generated query plan json
	// we subclass this object to more specific node types
	// 
	// a Node has two kinds of sub-nodes:
	// attributes - a loosely-type collection of primitive datatypes and Nodes, used to roundtrip the json
	// children - contains Nodes only, used to traverse the Node tree. 
	//   Note that a concrete subclass also contain node-specific fields which may also be contained in children
	
	public static abstract class Node implements Struct.ToJson {
		private String jsonType;
		private boolean isArray;
		protected Map<String, Object> attributes = new HashMap<>();
		protected List<Node> children = new ArrayList<>();
		
		public Node(String jsonType, boolean isArray) {
			this.jsonType = jsonType;
			this.isArray = isArray;
		}
		public void addChild(Node c) {
			children.add(c);
		}
		public String getJsonType() { 
			return jsonType;
		}
		public String toJson() {
			String s;
			if (isArray) {
				// attributes ?
				if (attributes.size() > 0) {
					throw new IllegalStateException("didn't expect attributes on array");
				}
				s = "[";
				for (int i=0; i<children.size(); i++) {
					Node c = children.get(i);
					s += (i==0 ? "" : ", ") + // children.get(i).toJson();
						"{ \"" + Text.escapeJavascript(c.jsonType) + "\": " + c.toJson() + "}";
				}
				s += "]";
				
			} else {
				s = "{";
				boolean f = true;
				for (String k : attributes.keySet()) {
					Object v = attributes.get(k);
					if (v==null) { continue; } // TODO
					s += (f == true ? "": ", ") + "\"" + Text.escapeJavascript(k) + "\": ";
					s += (v == null ? "null" :
						((v instanceof Number) ? ((Number) v).toString() :
						((v instanceof Boolean) ? ((Boolean) v).toString() :
						((v instanceof String) ? "\"" + Text.escapeJavascript((String) v) + "\"" :
						((v instanceof Node) ? ((Node) v).toJson() : // all of these implement Struct.ToJson, but going to list them here anyway 
						((v instanceof CostInfo) ? ((CostInfo) v).toJson() :
						((v instanceof NameList) ? ((NameList) v).toJson() :
							
						" HUH " + v.getClass().getName() )))))));
					f = false;
				}
				List<Object> remaining = new ArrayList<>();
				for (Object v : children) {
					if (v instanceof Node) {
						Node n = (Node) v;
						s += (f == true ? "": ", ") + "\"" + Text.escapeJavascript(n.jsonType) + "\": " + n.toJson(); // -CHILD 
					} else {
						remaining.add(v);
					}
				}
				if (remaining.size() > 0) {
					throw new IllegalStateException("didn't expect remaining children here");
				}
				s += "}";
			}
			return s;
		}
		
		protected Stream<Node> reverseStream(List<Node> children) {
			// why the hell isn't this on https://stackoverflow.com/questions/24010109/java-8-stream-reverse-order 
			// ?
			return IntStream.range(0, children.size())
				.mapToObj(i -> children.get(children.size() - i - 1)); 
		}

	}
	
	public static class QueryBlockNode extends Node {
		
		public Long selectId;
		public Object message;
		public CostInfo costInfo;
		public Node queryNode;
		
		public QueryBlockNode() {
			super("query_block", false);
		}


	}
	
	public static class UnionResultNode extends Node {
		public String tableName;
		public List<QuerySpecificationNode> querySpecifications;
		public UnionResultNode() {
			super("union_result", false); // TODO subtypes
		}
		

	}
	
	public static class ListNode extends Node {
		public ListNode(String name, List<? extends Node> children) {
			super(name, true);
			for (Node n : children) {
				this.children.add(n);
			}
		}
	}
	
	public static class QuerySpecificationNode extends Node {
		public QueryBlockNode queryBlock;

		public QuerySpecificationNode() {
			super("query_specification", false);
		}
	}
	
	public static class DuplicatesRemovalNode extends Node {
		public Boolean usingTemporaryTable;
		public NestedLoopNode nestedLoop;
		public DuplicatesRemovalNode() {
			super("duplicates_removal", false); 
		}
		
	}
	
	public static class OrderingOperationNode extends Node {
		public boolean usingTemporaryTable = false;
		public boolean usingFilesort = false;

		public NestedLoopNode nestedLoop;
		public OrderingOperationNode() {
			super("ordering_operation", false); 
		}
	}
	
	public static class GroupingOperationNode extends Node {
		public boolean usingFilesort = false;
		public NestedLoopNode nestedLoop;
		public GroupingOperationNode() {
			super("gouping_operation", false); 
		}
	}

	
	public static class NestedLoopNode extends Node {
		
		public List<TableNode> tables = new ArrayList<>();
		
		public NestedLoopNode() {
			super("nestedLoop", true);
		}
		
	}
	
	public static class TableNode extends Node {
		public CostInfo costInfo;
		public AccessTypeEnum accessType;
		public String tableName;
		public String key;
		public Long rowsExaminedPerScan;
		public Long rowsProducedPerJoin;
		public AttachedSubqueriesNode attachedSubqueries;
		public MaterialisedFromSubqueryNode materialisedFromSubquery;

		public TableNode() {
			super("table", false);
		}
		
	}
	
	public static class AttachedSubqueriesNode extends Node {
		public QuerySpecificationNode querySpecification;

		public AttachedSubqueriesNode() {
			super("attached_subqueries", true);
		}
		

	}
	
	public static class MaterialisedFromSubqueryNode extends Node {
		public QueryBlockNode queryBlock;
		public boolean dependent;
		public boolean cacheable;
		public boolean usingTemporaryTable;

		public MaterialisedFromSubqueryNode() {
			super("materialized_from_subquery", false);
		}
	}
	

	public static class BoxVisitor {
		
		// before node or children have been traversed
		public void preVisit(Box b) {
			
		};

		// traverse a node
		public void visit(Box b) {
			
		};

		// after children have been traversed
		public void postVisit(Box b) {
			
		};

	}
	
	// a box holds other boxes, and basically translates to a rectangle in the generated diagram
	// a box may have a parent box which is outside the bounds of the box; all children are within the bounds of the box
	
	public static class Box {


		Box connectedTo;   // draw a line to this box
		Box layoutParent;  // X and Y co-ordinates are relative to this box ( layoutParent can be null, or parent, or parent.parent ... )
		Double connectedWeight; // is converted to line width once we know the min/max
		
		int posX = 0, posY = 0; // relative to parent
		int width, height;
		String shape = "rect";
		String label;
		String tooltip;
		
		String targetPort;
		int edgeStartX, edgeStartY; // draw edges from this box from this point
		int edgeEndX, edgeEndY;     // draw edges to this box to this point
		
		String cssClass;
		Color stroke = null; // Color.BLACK;
		Color fill = null; // new Color(0, 0, 0, 0);
		Color textColor = null; // Color.BLACK;
		List<String> strokeDashArray = null;
		String textAnchor = null;
		
		
		List<Box> children = new ArrayList<Box>();
		
		public void setLabel(String label) {
			this.label = label;
		}
		public void setSize(int width, int height) {
			this.width = width;
			this.height = height;
		}
		public void setStroke(Color c) { this.stroke = c; }
		public void setFill(Color c) { this.fill = c; }
		public void setTextColor(Color c) { this.textColor = c; }
		
		public void connectTo(Box connectedTo, String targetPort) {
			this.connectedTo = connectedTo;
			this.targetPort = targetPort;
		
			//this.edgeStartX = edgeX;
			//this.edgeStartY = edgeY;
		}
		public void setParentAndPosition(Box parent, int posX, int posY) {
			if (this.layoutParent!=null) {
				throw new IllegalStateException("twice");
			}
			this.layoutParent = parent;
			this.posX = posX;
			this.posY = posY;
			// add child links at the same time
			parent.children.add(this);
		}
		public void setPosition(int posX, int posY) {
			this.posX = posX;
			this.posY = posY;
		}
		public void setEdgeStartPosition(int edgeStartX, int edgeStartY) {
			this.edgeStartX = edgeStartX;
			this.edgeStartY = edgeStartY;
		}
		public void setEdgeEndPosition(int edgeEndX, int edgeEndY) {
			this.edgeEndX = edgeEndX;
			this.edgeEndY = edgeEndY;
		}


		public void addAll(List<Box> children) {
			for (Box c : children) {
				if (c.layoutParent!=null) {
					throw new IllegalStateException("twice");
				}
				this.children.add(c);
			}
			// this.children.addAll(children);
		}

		
		public int getWidth() { 
			return width; 
		}
		public int getHeight() { 
			return height; 
		}
		public int[] getAbsolutePortPosition(String d) {
			if (d.equals("s")) { return new int[] { getAbsoluteX() + (getWidth() / 2), getAbsoluteY() + getHeight() }; }
			else if  (d.equals("w")) { return new int[] { getAbsoluteX(), getAbsoluteY() + (getHeight() / 2) }; }
			else if  (d.equals("e")) { return new int[] { getAbsoluteX() + getWidth(), getAbsoluteY() + (getHeight() / 2) }; }
			else {
				throw new IllegalArgumentException("Unknown port '" + d + "'");
			}
			
		}
		
		public int getAbsoluteX() {
			int x = posX;
			Box rel = layoutParent;
			while (rel != null) {
				x += rel.posX;
				rel = rel.layoutParent;
			}
			return x;
		}

		public int getAbsoluteY() {
			int y = posY;
			Box rel = layoutParent;
			while (rel != null) {
				y += rel.posY;
				rel = rel.layoutParent;
			}
			return y;
		}

		public void traverse(BoxVisitor visitor) {
			visitor.preVisit(this);
			visitor.visit(this);
			for (Box c : children) {
				c.traverse(visitor);
			}
			visitor.postVisit(this);
		}
		
		public String getShape() {
			return shape;
		}
		public void setShape(String shape) {
			this.shape = shape;
		}
		public String getTextAnchor() {
			return textAnchor;
		}
		public void setTextAnchor(String textAnchor) {
			this.textAnchor = textAnchor;
		}
		public String getCssClass() {
			return cssClass;
		}
		public void setCssClass(String cssClass) {
			this.cssClass = cssClass;
		}
		public List<String> getStrokeDashArray() {
			return strokeDashArray;
		}
		public void setStrokeDashArray(List<String> strokeDashArray) {
			this.strokeDashArray = strokeDashArray;
		}
		public String getTooltip() {
			return tooltip;
		}
		public void setTooltip(String tooltip) {
			this.tooltip = tooltip;
		}
		public Double getConnectedWeight() {
			return connectedWeight;
		}
		public void setConnectedWeight(Double connectedWeight) {
			this.connectedWeight = connectedWeight;
		}
		
	}
	
	public static class CBox extends Box {
		public CBox() {
			super();
			setStroke(new Color(0, 0, 0, 0)); // add class ?			
		}
	}

	
	public static class Layout {
		private QueryBlockNode topNode;
		public Layout(QueryBlockNode topNode) {
			this.topNode = topNode;
			
			// so I'm either going to be all the layout logic in here
			// or split it across the parsed nodes.
			// so which am I going to prefer
			//   if (ThisKIndOfNode) { doThis }
			// or
			//   ThisKindOfNode.doThis
			// how about
			//   doThis(node)
			
			// let's see how we go
			// layout(topNode);
		}
		
		public Box getLayoutBox() {
			Box b = layout(topNode, "query_block", true);
			// probably need to reposition everything so that it starts at 0,0
			return b;
		}

		// make these all protected later
		public Box layout(QueryBlockNode n, String queryBlockLabel) {
			return layout(n, queryBlockLabel, false);
		}
		
		public Box layout(QueryBlockNode n, String queryBlockLabel, boolean topNode) {
			Node c = n.queryNode; // 1 child only
			
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
				String label = queryBlockLabel + (topNode ? "" : " #" + n.selectId); // n.selectId == null || n.selectId == 1
				String clazz = (topNode ? " topNode" : ""); // n.selectId == null || n.selectId == 1
				String tooltip = "Select ID: " + n.selectId + "\n" +
				  (n.costInfo == null ? "" :"Query cost: " + n.costInfo.getQueryCost() + "\n");
				
				
				Box lb = new Box(); // label box
				lb.setCssClass("queryBlock" + clazz);
				if (cb == null) {
					lb.setParentAndPosition(ob, 0, 0);
					ob.setEdgeStartPosition(50, 0);
				} else {
					lb.setParentAndPosition(ob, cb.edgeStartX - 50, 0);
					ob.setEdgeStartPosition(cb.edgeStartX, 0);
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
		
		public Box layout(UnionResultNode n) {
			List<QuerySpecificationNode> qsnList = n.querySpecifications;
			
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
		
		public Box layout(AttachedSubqueriesNode n) {
			// @TODO multiple qsns
			List<QuerySpecificationNode> qsnList = Collections.singletonList(n.querySpecification);
			
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
		
		
		public Box layout(DuplicatesRemovalNode n) {
			NestedLoopNode nln = n.nestedLoop; // 1 child only
			Box qb = layout(nln);
			
			int w = qb.getWidth();
			int h = qb.getHeight();
			
			Box ob = new CBox(); // outer box
			ob.setSize(w, h + 10 + 45);
			ob.setEdgeStartPosition(qb.edgeStartX,  0);

			Box cb = new CBox(); // container box
			cb.setParentAndPosition(ob,  qb.edgeStartX - 40, 0);
			cb.setSize(80, n.usingTemporaryTable ? 55 : 40);
			
			Box lb = new Box(); // label box
			lb.setParentAndPosition(ob, qb.edgeStartX - 40, 0);
			lb.setCssClass("duplicatesRemoval");
			lb.setLabel("DISTINCT"); 
			lb.setSize(80, 40);
			
			if (n.usingTemporaryTable) {
				Box ttBox = new CBox(); 
				ttBox.setLabel("tmp table");
				ttBox.setSize(80, 10);
				ttBox.setCssClass("tempTableName");
				ttBox.setTextAnchor("start");
				ttBox.setParentAndPosition(ob, qb.edgeStartX - 40, 45);
			}

			qb.connectTo(cb, "s"); // "up", 50, 30
			qb.setParentAndPosition(ob, 0, 40);
			
			return ob;

		}
		
		public Box layout(NestedLoopNode n) {
			List<Box> nestedLoopBoxes = new ArrayList<Box>();
			List<TableNode> qsnList = n.tables;
			
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
				TableNode qsn = n.tables.get(i);
				b.setShape("nestedLoop");
				b.setSize(60, 60); // diamond
				b.setParentAndPosition(ob, tb.posX + tb.edgeStartX /*(tableWidths.get(i)/2)*/ - 30, 50); // centered above table beneath it
				b.setTooltip("nested_loop\n\n" +
				   "Prefix Cost: " + qsn.costInfo.getPrefixCost());
				nestedLoopBoxes.add(b);
				
				Box lb = new CBox(); // label box
				lb.setCssClass("queryCost");
				lb.setParentAndPosition(b, -10, -15);
				lb.setLabel(String.valueOf(qsn.costInfo.getPrefixCost())); 
				lb.setTextAnchor("start");
				lb.setSize(40, 10);

				if (i == tableBoxes.size() - 1) {
					lb = new CBox(); // label box
					lb.setCssClass("queryCost");
					lb.setParentAndPosition(b, 40, -10);
					lb.setLabel(String.valueOf(qsn.rowsProducedPerJoin) +
						(qsn.rowsProducedPerJoin == 1 ? " row" : " rows")); 
					lb.setTextAnchor("start");
					lb.setSize(25, 10);
				} else {
					lb = new CBox(); // label box
					lb.setCssClass("queryCost");
					lb.setParentAndPosition(b, 65, 15);
					lb.setLabel(String.valueOf(qsn.rowsProducedPerJoin) +
						(qsn.rowsProducedPerJoin == 1 ? " row" : " rows")); 
					lb.setTextAnchor("start");
					lb.setSize(25, 10);
				}

				
				if (i == 1) {
					Box firstTableBox = tableBoxes.get(0);
					firstTableBox.connectTo(b, "w"); // "upRight", 0, 15
					firstTableBox.setConnectedWeight((double) qsnList.get(0).rowsExaminedPerScan);
				} else {
					prevNestedLoopBox.connectTo(b, "w"); // "right", 0, 15
					prevNestedLoopBox.setConnectedWeight((double) qsnList.get(i - 1).rowsExaminedPerScan); // @TODO not sure about this either
					prevNestedLoopBox.setEdgeStartPosition(60, 30);
				}
				
				Box tableBox = tableBoxes.get(i);
				tableBox.setConnectedWeight((double) qsnList.get(i).rowsExaminedPerScan);
				tableBox.connectTo(b, "s"); // "up", 15, 30
				
				prevNestedLoopBox = b;
			}

			ob.setEdgeStartPosition(prevNestedLoopBox.posX + 30, 50); // although the edge has already been drawn, so this is the edge end position really. maybe not. 
			return ob;
		}
				
		public Box layout(TableNode n) {
			
			int w = (n.accessType==AccessTypeEnum.FULL_TABLE_SCAN ? 100 :
				(n.accessType==AccessTypeEnum.FULL_INDEX_SCAN ? 100 :
				(n.accessType==AccessTypeEnum.NON_UNIQUE_KEY ? 150 :
				(n.accessType==AccessTypeEnum.UNIQUE_KEY ? 125 : 100)))); 
			int h = 60;
			Box ob = new CBox(); // outer box
			
			if (n.materialisedFromSubquery == null) {
				if (n.tableName != null) {
					Box lb = new CBox(); // label box
					lb.setCssClass("tableName");
					lb.setParentAndPosition(ob, 0, 32);
					lb.setLabel(n.tableName); 
					lb.setSize(w, 14);
				}
				if (n.key != null) {
					Box lb = new CBox(); // label box
					lb.setCssClass("tableKey"); 
					lb.setParentAndPosition(ob, 0, 46);
					lb.setLabel(n.key); 
					lb.setSize(w, 14);
				}
			} else {
				QueryBlockNode queryBlock = n.materialisedFromSubquery.queryBlock;
				Box qb;
				qb = layout(queryBlock, null); // query_blocks in materialised queries aren't drawn for some reason
				// reset to 0,0

				RangeVisitor rv = new RangeVisitor();
				qb.traverse(rv);
				logger.info("materialised subquery range [" + rv.getMinX() + ", " + rv.getMinY() + "] - [" + rv.getMaxX() + ", " + rv.getMaxY() + "]");
				// qb.posX -= rv.getMinX();
				// qb.posY -= rv.getMinY();

				h = 80 + qb.height + 20; // 20px padding bottom
				w = Math.max(w, qb.width + 20); // 10px padding left and right

				Box lb = new CBox();
				lb.setCssClass("dotted"); // dotted line box
				lb.setStroke(new Color(140, 140, 140)); // #8c8c8c
				lb.setStrokeDashArray(Arrays.asList(new String[] { "4" }));
				lb.setParentAndPosition(ob, 0, 0);
				lb.setSize(w, h);
				
				// qb after lb in the diagram so that tooltips work
				qb.setParentAndPosition(ob, 10 - rv.getMinX(), 85);

				if (n.tableName != null) {
					lb = new CBox(); // label box
					lb.setCssClass("materialisedTableName");
					lb.setFill(new Color(232, 232, 232));
					lb.setParentAndPosition(ob, 0, 32);
					lb.setLabel(n.tableName + " (materialised)"); 
					lb.setSize(w, 20);
				}
				if (n.key != null) {
					lb = new CBox(); // label box
					lb.setCssClass("tableKey"); 
					lb.setParentAndPosition(ob, 0, 52);
					lb.setLabel(n.key); 
					lb.setSize(w, 14);
				}

			}

			ob.setEdgeStartPosition(w / 2, 0);
			
			Box b = new Box(); 
			b.setParentAndPosition(ob, 0, 0);
			b.setSize(w, 30);
			// b.setTextColor(Color.WHITE);
			b.setCssClass("table" + (n.accessType==AccessTypeEnum.FULL_TABLE_SCAN ? " fullTableScan" :
				(n.accessType==AccessTypeEnum.FULL_INDEX_SCAN ? " fullIndexScan" :
				(n.accessType==AccessTypeEnum.NON_UNIQUE_KEY ? " nonUniqueKey" :
				(n.accessType==AccessTypeEnum.UNIQUE_KEY ? " uniqueKey" : "")))));
			b.setLabel((n.accessType==AccessTypeEnum.FULL_TABLE_SCAN ? "Full Table Scan" :
				(n.accessType==AccessTypeEnum.FULL_INDEX_SCAN ? "Full Index Scan" :
				(n.accessType==AccessTypeEnum.NON_UNIQUE_KEY ? " Non-Unique Key Lookup" :
				(n.accessType==AccessTypeEnum.UNIQUE_KEY ? "Unique Key Lookup" : "")))));
			
			CostInfo costInfo = n.costInfo;
			if (costInfo != null) {
				double cost= (costInfo.getEvalCost() == null ? (double) 0 : costInfo.getEvalCost()) +
					(costInfo.getReadCost() == null ? (double) 0 : costInfo.getReadCost());
				Box lb = new CBox(); // label box
				lb.setCssClass("lhsQueryCost"); lb.setTextAnchor("start");
				lb.setParentAndPosition(ob, 0, -15);
				lb.setLabel(String.valueOf(cost)); 
				lb.setSize(w/2, 10);
			}
			if (n.rowsExaminedPerScan != null) {
				Box lb = new CBox(); // label box
				lb.setCssClass("rhsQueryCost");  lb.setTextAnchor("end");
				lb.setParentAndPosition(ob, w/2, -15);
				lb.setLabel(String.valueOf(n.rowsExaminedPerScan) + 
					(n.rowsExaminedPerScan == 1 ? " row" : " rows")); 
				lb.setSize(w/2, 10);
			}
			
			if (n.attachedSubqueries != null) {
				Box qb = layout(n.attachedSubqueries);
				qb.setParentAndPosition(ob, w + 50, 0);
				qb.connectTo(b, "e");
				w = w + 50 + qb.getWidth();
				h = Math.max(h,  qb.getHeight());
			
			}
			
			ob.setSize(w, h);
			return ob;
			
		}
		public Box layout(OrderingOperationNode n) {

			NestedLoopNode nln = n.nestedLoop; // 1 child only
			Box cb = layout(nln);
			
			int w = cb.getWidth();
			int h = cb.getHeight();
			
			Box ob = new CBox(); // outer box, nestedLoop exit edge is drawn inside the nestedLoop box
			ob.setSize(w, h + 40);
			ob.setEdgeStartPosition(cb.edgeStartX,  0);
			
			if (n.usingTemporaryTable) { 
				Box tb = new CBox(); // label box
				tb.setParentAndPosition(ob, cb.edgeStartX - 40, 45); 
				tb.setSize(w, 10);
				tb.setCssClass("tempTableName");
				tb.setLabel("tmp table"); 
			}
			
			Box lb = new Box(); // label box
			lb.setParentAndPosition(ob, cb.edgeStartX - 40, 0);
			lb.setCssClass("orderingOperation");
			lb.setLabel("ORDER"); 
			lb.setSize(80, 40);
			lb.setTooltip("Ordering operation\n\n" +
				"Using Filesort: " + n.usingFilesort);

			cb.connectTo(lb, "s"); // "up", 50, 30
			cb.setParentAndPosition(ob, 0, 40);
			
			
			return ob;
			
		}
		public Box layout(GroupingOperationNode n) {
			
			NestedLoopNode nln = n.nestedLoop; // 1 child only
			Box cb = layout(nln);
			
			int w = cb.getWidth();
			int h = cb.getHeight();
			
			Box ob = new CBox(); // outer box 
			ob.setSize(w, h + 40);
			ob.setEdgeStartPosition(cb.edgeStartX,  0);
			
			Box lb = new Box(); // label box
			lb.setParentAndPosition(ob, cb.edgeStartX - 40, 0);
			lb.setCssClass("groupingOperation");
			lb.setLabel("GROUP"); 
			lb.setSize(80, 40);

			cb.connectTo(lb, "s"); // "up", 50, 30
			cb.setParentAndPosition(ob, 0, 40);
			
			return ob;
			
			
		}
		
		public Box layout(QuerySpecificationNode n, String queryBlockLabel) {

			QueryBlockNode qb = n.queryBlock;
			
			Box cb = layout(qb, queryBlockLabel);
			
			int w = cb.getWidth();
			int h = cb.getHeight();
			
			Box ob = new CBox(); 
			ob.setSize(w, h + 40);
			ob.setEdgeStartPosition(cb.edgeStartX,  40);

			cb.setParentAndPosition(ob, 0, 40);
			return ob;
		}
		
		public Box layout(Node n) {
			throw new UnsupportedOperationException("layout for node " + n.getClass().getName() + " not implemented");
		}
		
		protected <T> Stream<T> reverseStream(List<T> children) {
			// why the hell isn't this on https://stackoverflow.com/questions/24010109/java-8-stream-reverse-order 
			// ?
			return IntStream.range(0, children.size())
				.mapToObj(i -> children.get(children.size() - i - 1)); 
		}

		
	}
	
	public static class PlanParser {

		private QueryBlockNode topNode;
		String json;
		
		public PlanParser(String json, String server_version) throws ParseException {
			this.json = json;
			this.topNode = getNode(json);
		}
		
		public QueryBlockNode getNode(String json) throws ParseException {
			JSONParser jp = new JSONParser();
			JSONObject obj = (JSONObject) jp.parse(json);
			
			// ok then
			QueryBlockNode n;
			if (obj.containsKey("query_block")) {
				n = parseQueryBlock((JSONObject) obj.get("query_block"));
			} else {
				throw new IllegalArgumentException("expected query_block");
			}
			return n;
		}

		// query_block
		private QueryBlockNode parseQueryBlock(JSONObject obj) {
			QueryBlockNode n = new QueryBlockNode();
			n.selectId = toLong(obj.get("select_id"));
			n.message = obj.get("message");
			
			n.attributes.put("selectId", obj.get("select_id"));
			n.attributes.put("message", obj.get("message"));
			if (obj.containsKey("cost_info")) {
				n.costInfo = parseCostInfo((JSONObject) obj.get("cost_info"));
				n.attributes.put("costInfo", n.costInfo); 
			}
			
			// ok so my theory is that the qbn can contain exactly one of these things
			// which is always drawn underneath the query block rectangle
			
			if (obj.containsKey("union_result")) {
				Node cn = parseUnionResult((JSONObject) obj.get("union_result"));
				n.queryNode = cn;
				n.addChild(cn);
			} else if (obj.containsKey("duplicates_removal")) { // DISTINCT node
				Node cn = parseDuplicatesRemoval((JSONObject) obj.get("duplicates_removal"));
				n.queryNode = cn;
				n.addChild(cn);
				// other nodes
			} else if (obj.containsKey("table")) {
				Node cn = parseTable((JSONObject) obj.get("table"));
				n.queryNode = cn;
				n.addChild(cn);
			} else if (obj.containsKey("ordering_operation")) {
				Node cn = parseOrderingOperation((JSONObject) obj.get("ordering_operation"));
				n.queryNode = cn;
				n.addChild(cn);
			} else if (obj.containsKey("grouping_operation")) {
				Node cn = parseGroupingOperation((JSONObject) obj.get("grouping_operation"));
				n.queryNode = cn;
				n.addChild(cn);
			} else {
				// can have 'no tables used' if this is a SELECT without a table
				
				// throw new IllegalStateException("Expected 'union_result', 'duplicates_removal', 'table', 'ordering_operation', 'grouping_operation' in query block " + n.selectId);
			}
			return n;
		}
		
		private Long toLong(Object object) {
			return object == null ? null : ((Number) object).longValue();
		}

		private CostInfo parseCostInfo(JSONObject obj) {
			CostInfo ci = new CostInfo();
			if (obj.containsKey("query_cost")) { ci.setQueryCost(Double.parseDouble((String) obj.get("query_cost"))); }
			if (obj.containsKey("read_cost")) { ci.setReadCost(Double.parseDouble((String) obj.get("read_cost"))); }
			if (obj.containsKey("eval_cost")) { ci.setEvalCost(Double.parseDouble((String) obj.get("eval_cost"))); }
			if (obj.containsKey("prefix_cost")) { ci.setPrefixCost(Double.parseDouble((String) obj.get("prefix_cost"))); }
			ci.setDataReadPerJoin((String) obj.get("data_read_per_join"));
			return ci;
		}

		
		private UnionResultNode parseUnionResult(JSONObject obj) {
			UnionResultNode n = new UnionResultNode();
			n.tableName = (String) obj.get("table_name");
			
			n.attributes.put("usingTemporaryTable", obj.get("using_temporary_table"));
			n.attributes.put("tableName", obj.get("table_name"));
			n.attributes.put("accessType", obj.get("access_type"));
			if (obj.containsKey("query_specifications")) {
				List<QuerySpecificationNode> qsnList = parseQuerySpecifications((JSONArray) obj.get("query_specifications"));
				n.querySpecifications = qsnList;
				n.addChild(new ListNode("query_specifications", qsnList));
			} else {
				// other nodes
			}
			return n;
		}
		
		private List<QuerySpecificationNode> parseQuerySpecifications(JSONArray qs) {
			List<QuerySpecificationNode> qsnList = new ArrayList<>();
			// QuerySpecificationsNode n = new QuerySpecificationsNode();
			for (Object o : qs) {
				JSONObject cobj = (JSONObject) o;
				QuerySpecificationNode cn = parseQuerySpecification((JSONObject) cobj);
				qsnList.add(cn);
			}
			return qsnList;
		}
		
		private QuerySpecificationNode parseQuerySpecification(JSONObject obj) {
			QuerySpecificationNode n = new QuerySpecificationNode();
			n.attributes.put("dependent", obj.get("dependent"));
			n.attributes.put("cacheable", obj.get("cacheable"));
			if (obj.containsKey("query_block")) {
				QueryBlockNode cn = parseQueryBlock((JSONObject) obj.get("query_block"));
				n.queryBlock = cn;
				n.addChild(cn);
			} else {
				throw new IllegalArgumentException("expected query_block in query_specification: " + obj.toString());
			}
			return n;
		}
		
		private MaterialisedFromSubqueryNode parseMaterialisedFromSubquery(JSONObject obj) {
			MaterialisedFromSubqueryNode n = new MaterialisedFromSubqueryNode();
			n.dependent = (Boolean) obj.get("dependent");
			n.cacheable = (Boolean) obj.get("cacheable");
			n.usingTemporaryTable = (Boolean) obj.get("using_temporary_table");
			
			n.attributes.put("dependent", obj.get("dependent"));
			n.attributes.put("cacheable", obj.get("cacheable"));
			n.attributes.put("usingTemporaryTable", obj.get("using_temporary_table")); // boolean
			if (obj.containsKey("query_block")) {
				QueryBlockNode cn = parseQueryBlock((JSONObject) obj.get("query_block"));
				n.queryBlock = cn;
				n.addChild(cn);
			} else {
				throw new IllegalArgumentException("expected query_block in query_specification: " + obj.toString());
			}
			
			return n;
		}
		 
				
		
		private DuplicatesRemovalNode parseDuplicatesRemoval(JSONObject obj) {
			DuplicatesRemovalNode n = new DuplicatesRemovalNode();
			n.usingTemporaryTable = (Boolean) obj.get("using_temporary_table");
					
			n.attributes.put("usingTemporaryTable", obj.get("using_temporary_table"));
			n.attributes.put("usingFilesort", obj.get("using_filesort"));
			// n.attributes.put("selectId", obj.get("select_id"));
			// n.attributes.put("costInfo", parseCostInfo((JSONObject) obj.get("cost_info")));
			
			if (obj.containsKey("nested_loop")) {
				NestedLoopNode nln = parseNestedLoop((JSONArray) obj.get("nested_loop"));
				n.nestedLoop = nln;
				n.addChild(nln);
			} else {
				throw new IllegalArgumentException("expected nested_loop");
			}
			return n;
		}
		
		private NestedLoopNode parseNestedLoop(JSONArray loopItems) {
			NestedLoopNode n = new NestedLoopNode();
			
			// JSONArray loopItems = (JSONArray) obj.get("nested_loop");
			for (Object o : loopItems) {
				JSONObject cobj = (JSONObject) o;
				if (cobj.containsKey("table")) {
					TableNode cn = parseTable((JSONObject) cobj.get("table"));
					n.tables.add(cn);
					n.addChild(cn);
				} else {
					throw new IllegalArgumentException("expected table in nested_loop child");
				}
			}
			return n;
		}
		
		private OrderingOperationNode parseOrderingOperation(JSONObject obj) {
			OrderingOperationNode n = new OrderingOperationNode();
			if (obj.containsKey("using_temporary_table")) { n.usingTemporaryTable = (Boolean) obj.get("using_temporary_table"); }
			n.usingFilesort = (Boolean) obj.get("using_filesort");
					
			n.attributes.put("usingTemporaryTable", obj.get("using_temporary_table"));
			n.attributes.put("usingFilesort", obj.get("using_filesort"));
			// n.attributes.put("selectId", obj.get("select_id"));
			// n.attributes.put("costInfo", parseCostInfo((JSONObject) obj.get("cost_info")));
			
			if (obj.containsKey("nested_loop")) {
				NestedLoopNode nln = parseNestedLoop((JSONArray) obj.get("nested_loop"));
				n.nestedLoop = nln;
				n.addChild(nln);
			}
			return n;
		}
		
		private GroupingOperationNode parseGroupingOperation(JSONObject obj) {
			GroupingOperationNode n = new GroupingOperationNode();
			// n.usingTemporaryTable = (Boolean) obj.get("using_temporary_table");
			// n.attributes.put("usingTemporaryTable", obj.get("using_temporary_table"));
			
			if (obj.containsKey("using_filesort")) { n.usingFilesort = (Boolean) obj.get("using_filesort"); }
			
			n.attributes.put("usingFilesort", obj.get("using_filesort"));
			// n.attributes.put("selectId", obj.get("select_id"));
			// n.attributes.put("costInfo", parseCostInfo((JSONObject) obj.get("cost_info")));
			
			if (obj.containsKey("nested_loop")) {
				NestedLoopNode nln = parseNestedLoop((JSONArray) obj.get("nested_loop"));
				n.nestedLoop = nln;
				n.addChild(nln);
			}
			return n;
		}
		
		
		
		private NameList parseNameList(JSONArray a) {
			if (a==null) { return null; }
			NameList n = new NameList();
			for (int i=0; i<a.size(); i++) {
				n.add((String) a.get(i));
			}
			return n;
		}
		
		private TableNode parseTable(JSONObject obj) {
			TableNode n = new TableNode();
			
			// either a table table, or a materialized_from_subquery table (tableName is the alias I guess)
			n.tableName = (String) obj.get("table_name");
			n.attributes.put("tableName", obj.get("table_name"));
			
			
			n.attributes.put("distinct", obj.get("distinct")); // boolean
			n.attributes.put("usingIndex", obj.get("using_index")); // boolean
			
			n.accessType = AccessTypeEnum.fromJsonValue((String) obj.get("access_type"));
			n.attributes.put("accessType", obj.get("access_type")); // "ALL", "ref", "eq_ref"
			n.attributes.put("possibleKeys", parseNameList((JSONArray) obj.get("possible_keys"))); // List<String>
			if (obj.containsKey("key")) { n.key = (String) obj.get("key"); n.attributes.put("key",  obj.get("key")); } // "PRIMARY"
			n.attributes.put("usedKeyParts", parseNameList((JSONArray) obj.get("used_key_parts"))); // List<String>, subset of above ?
			n.attributes.put("usedColumns", parseNameList((JSONArray) obj.get("used_columns"))); // List<String>
			if (obj.containsKey("key_length")) { n.attributes.put("keyLength", Long.parseLong((String) obj.get("key_length"))); }
			if (obj.containsKey("ref")) { n.attributes.put("ref", parseNameList((JSONArray) obj.get("ref"))); } // List<String>
			
			if (obj.containsKey("rows_examined_per_scan")) { n.rowsExaminedPerScan = ((Number) obj.get("rows_examined_per_scan")).longValue(); n.attributes.put("rows_examined_per_scan", n.rowsExaminedPerScan); }
			if (obj.containsKey("rows_produced_per_join")) { n.rowsProducedPerJoin = ((Number) obj.get("rows_produced_per_join")).longValue(); n.attributes.put("rows_produced_per_join", n.rowsProducedPerJoin); }
			if (obj.containsKey("filtered")) { n.attributes.put("filtered", Double.parseDouble((String) obj.get("filtered"))); }
			n.attributes.put("index_condition", obj.get("index_condition"));
			
			if (obj.containsKey("cost_info")) { 
				n.costInfo = parseCostInfo((JSONObject) obj.get("cost_info")); 
				n.attributes.put("costInfo", n.costInfo); 
			}

			// @TODO object form ?
			n.attributes.put("attachedCondition", obj.get("attached_condition"));
			
			if (obj.containsKey("attached_subqueries")) {
				AttachedSubqueriesNode sn = new AttachedSubqueriesNode();
				n.attachedSubqueries = sn;
				n.addChild(sn);
				JSONArray qs = (JSONArray) obj.get("attached_subqueries");
				for (Object o : qs) {
					JSONObject cobj = (JSONObject) o;
					QuerySpecificationNode qsn = parseQuerySpecification((JSONObject) cobj);
					sn.querySpecification = qsn;
					sn.addChild(qsn);
				}
			}
			
			// can we have attached_subqueries and a materialised view at the same time ?
			
			if (obj.containsKey("materialized_from_subquery")) {
				MaterialisedFromSubqueryNode sn = parseMaterialisedFromSubquery((JSONObject) obj.get("materialized_from_subquery"));
				n.materialisedFromSubquery = sn;
				n.addChild(sn);
			}
			
			
			return n;
		}

		public String toJson() {
			
			return "{ \"" + Text.escapeJavascript(topNode.getJsonType()) + "\": " + topNode.toJson() + "}";
		}

	}
	
	
	public ExplainerSvg() {
		
	}
	
	public static class RangeVisitor extends BoxVisitor {
		
		int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
		int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
		
		double minWeight = Double.MAX_VALUE;
		double maxWeight = Double.MIN_VALUE;
		
		public RangeVisitor() {
		}
		public void visit(Box b) {
			int x = b.getAbsoluteX();
			int y = b.getAbsoluteY();
			int w = b.getWidth();
			int h = b.getHeight();
			
			minX = Math.min(minX, x);
			minY = Math.min(minY, y);
			maxX = Math.max(maxX, x + w);
			maxY = Math.max(maxY, y + h);
			
			Double cw = b.getConnectedWeight();
			if (cw != null) {
				minWeight = Math.min(minWeight, cw);
				maxWeight = Math.max(maxWeight, cw);
			}
		}
		
		public int getMinX() {
			return minX;
		}
		public int getMinY() {
			return minY;
		}
		public int getMaxX() {
			return maxX;
		}
		public int getMaxY() {
			return maxY;
		}
		public double getMinWeight() {
			return minWeight;
		}
		public double getMaxWeight() {
			return maxWeight;
		}
	}
	
	public static class ReweightVisitor extends BoxVisitor {
		double minWeight = Double.MAX_VALUE;
		double maxWeight = Double.MIN_VALUE;
		
		public ReweightVisitor(double minWeight, double maxWeight) {
			this.minWeight = minWeight;
			this.maxWeight = maxWeight;
		}
		
		public void visit(Box b) {
			Double cw = b.getConnectedWeight();
			Double maxWidth = 3d; // maybe change this depending on the magnitude of maxWeight;
			
			if (cw != null) {
				// turn into a line thickness.
				Double newWeight = 1 + ((cw - minWeight) / (maxWeight-minWeight)) * maxWidth;
				b.setConnectedWeight(newWeight);
			}
		}
	}
	
	public static class SvgBoxVisitor extends BoxVisitor {
		int indent = 0;
		PrintWriter pw;
		
		public SvgBoxVisitor(PrintWriter pw) {
			this.pw = pw;
		}
		
		@Override
		public void preVisit(Box b) {
			String s = "";
			if (indent==0) {
				// get range
				RangeVisitor rv = new RangeVisitor();
				b.traverse(rv);
				logger.info("range [" + rv.getMinX() + ", " + rv.getMinY() + "] - [" + rv.getMaxX() + ", " + rv.getMaxY() + "]");

				InputStream is = ExplainerTable.class.getResourceAsStream("/svg.css");
				String css;
				try {
					css = new String(StreamUtil.getByteArray(is));
				} catch (IOException e) {
					throw new IllegalStateException("IOException", e);
				}
				
				// add 1 to max as 1px lines on the border have 0.5px of that line outside the max co-ordinates
				s = "<!DOCTYPE html>\n" +
				  "<html>\n" +
				  "<head>\n" +
				  "<style>\n" + 
				  css + 
				  "</style>\n" +
				  "</head>\n" +
				  "<body>\n" +
				  "<svg width=\"" + (rv.getMaxX()+1) + "\" height=\"" + (rv.getMaxY()+1) + "\">\n" +
				  // svg arrowhead modified from http://thenewcode.com/1068/Making-Arrows-in-SVG
				  // and https://stackoverflow.com/questions/13626748/how-to-prevent-a-svg-marker-arrow-head-to-inherit-paths-stroke-width
				  "<defs>\n" +
				  "    <marker id=\"arrowhead\" markerWidth=\"12\" markerHeight=\"7\" refX=\"0\" refY=\"3.5\" orient=\"auto\" markerUnits=\"userSpaceOnUse\">\n" +
				  "      <polygon points=\"0 0, 12 3.5, 0 7\" />\n" +
				  "    </marker>\n" +
				  "  </defs>";
				indent += 4;
			}
			for (int i=0; i<indent; i++) { s += " "; }
			
			s += "<g" + ( b.cssClass == null ? "" : " class=\"" + b.cssClass + "\"") + ">\n"; // SVG group
			pw.print(s);
		}

		String toHex(Color c) {
			if (c.getAlpha() == 0) { return "transparent"; }
			String hex = String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
			if (c.getAlpha() != 255) {
				hex += String.format("#%02x", c.getAlpha());
			}
			return hex;
		}
		
		@Override
		public void visit(Box b) {
			// TODO Auto-generated method stub
			String is = "";
			for (int i=0; i<indent; i++) { is += " "; }

			int x = b.getAbsoluteX();
			int y = b.getAbsoluteY();
			
			String s = "";
			if (b.shape.equals("rect")) {
				s += is + "    <rect x=\"" + x + "\"" +
				    " y=\"" + y + "\"" +
				    " width=\"" + b.getWidth() + "\"" +
				    " height=\"" + b.getHeight() + "\"" +
					" style=\"" + 
				      (b.stroke == null ? "" : "stroke:" + toHex(b.stroke) + "; ") +
					  (b.fill == null ? "" : "fill:" + toHex(b.fill)+ "; ") +
					  (b.strokeDashArray == null ? "" : "stroke-dasharray:" + Text.escapeHtml(Text.join(b.strokeDashArray, " ")) + "; ") +
					"\"";
				s += (b.tooltip == null ? "/>\n" : ">\n" +
					is + "        <title>" + Text.escapeHtml(b.tooltip) + "</title>\n" +
					is + "    </rect>\n");
			} else if (b.shape.equals("nestedLoop")) {
				// s += is + "<path fill=\"none\" stroke=\"black\" d=\"M 30,0 L 60 30 L 30 60 L 0 30 L 30 0\"></path>\n";
				s += is + "<path fill=\"white\" stroke=\"black\" d=\"M " + (x + 30) + "," + y + " l 30 30 l -30 30 l -30 -30 l 30 -30\">";
				s += is + "    <title>" + Text.escapeHtml(b.tooltip)+ "</title>\n"; 
				s += is + "</path>\n";
				s += is + "<text x=\"" + (x + 30) + "\" y=\"" + (y + 25) + "\" font-size=\"10px\" dominant-baseline=\"middle\" text-anchor=\"middle\">nested</text>\n";
				s += is + "<text x=\"" + (x + 30) + "\" y=\"" + (y + 35) + "\" font-size=\"10px\" dominant-baseline=\"middle\" text-anchor=\"middle\">loop</text>\n";

			}
			if (b.label != null) {
				int tx = (b.textAnchor == null ? (x + (b.getWidth()/2)) :
					(b.textAnchor.equals("start") ? (x) :
					(b.textAnchor.equals("end") ? (x + b.getWidth()) : 0)));
						
				s += is + "    <text x=\"" + tx + "\"" +
			      " y=\"" + (y + (b.getHeight()/2)) + "\"" +
				  " style=\"" +
			        (b.textColor == null ? "" : "fill:" + toHex(b.textColor)+ ";") + 
			        (b.textAnchor == null ? "" : "text-anchor:" + b.textAnchor + ";") +
			      "\">" + 
				  Text.escapeHtml(b.label) + "</text>\n";
			}
			if (b.connectedTo != null) {
				Box ctb = b.connectedTo;
				int[] lineTo;
				double strokeWeight = b.connectedWeight == null ? 1 : b.connectedWeight; 
				if (b.targetPort.equals("sv")) {
					lineTo = ctb.getAbsolutePortPosition("s");
					lineTo[0] = x + b.edgeStartX;
				} else {
					lineTo = ctb.getAbsolutePortPosition(b.targetPort);
				}
				
				// still looks a bit weird, maybe something like
				//   https://stackoverflow.com/questions/27254640/svg-marker-scale-only-one-dimension-with-stroke-width
				// to scale the arrow cap
				
				if ((x + b.edgeStartX) == lineTo[0]) {
					// adjust end of line to leave room for arrowhead
					// (arrow is outside the polyline otherwise we don't get a sharp point)
					int a = (y + b.edgeStartY < lineTo[1]) ? -12 : 12; 
					s += is + "    <polyline points=\"" + (x + b.edgeStartX) + "," + (y + b.edgeStartY) + " " +
				      (lineTo[0]) + "," + (lineTo[1] + a) + "\"" +
					  " style=\"stroke:#000000; stroke-width:" + strokeWeight + ";\" marker-end=\"url(#arrowhead)\"/>\n";
				} else if ((y + b.edgeStartY == lineTo[1])) {
					int a = (x + b.edgeStartX < lineTo[0]) ? -12 : 12;
					s += is + "    <polyline points=\"" + (x + b.edgeStartX) + "," + (y + b.edgeStartY) + " " +
				      (lineTo[0] + a) + "," + (lineTo[1]) + "\"" +
					  " style=\"stroke:#000000; stroke-width:" + strokeWeight + ";\" marker-end=\"url(#arrowhead)\"/>\n";
				} else if (y + b.edgeStartY > lineTo[1]) { // up and horizontal
					int a = (x + b.edgeStartX < lineTo[0]) ? -12 : 12; 
					s += is + "    <polyline points=\"" + (x + b.edgeStartX) + "," + (y + b.edgeStartY) + " " +
				      (x + b.edgeStartX) + "," + (lineTo[1]) + " " + 
					  (lineTo[0] + a) + "," + (lineTo[1]) + "\"" +
					  " style=\"stroke:#000000; stroke-width:" + strokeWeight + "; fill: none;\" marker-end=\"url(#arrowhead)\"/>\n";
				} else {  // horizontal and down
					int a = (y + b.edgeStartY < lineTo[1]) ? -12 : 12;
					s += is + "    <polyline points=\"" + (x + b.edgeStartX) + "," + (y + b.edgeStartY) + " " +
				      (lineTo[0]) + "," + (y + b.edgeStartY) + " " + 
					  (lineTo[0]) + "," + (lineTo[1] + a) + "\"" +
					  " style=\"stroke:#000000; stroke-width:" + strokeWeight + "; fill: none;\" marker-end=\"url(#arrowhead)\"/>\n";
				}
			}
			
			pw.print(s);
			indent += 4;
		}

		@Override
		public void postVisit(Box b) {
			String s = "";
			indent -= 4;
			for (int i=0; i<indent; i++) { s += " "; }
			s += "</g>\n"; // end SVG group
			if (indent==4) {
				indent -= 4;
				s += "</svg>\n" +
				"</body>\n" +
				"</html>\n";
			}
			pw.print(s);
		}
		
	}
	
	public static void main(String args[]) throws IOException, ParseException {
		Log4jCliConfiguration lcc = new Log4jCliConfiguration();
		lcc.init("[explain-to-image]", null);
		
		// InputStream is = ExplainerSvg.class.getResourceAsStream("/sakila-7g.json");
		InputStream is = ExplainerSvg.class.getResourceAsStream("/somequery.json");
		ByteArrayOutputStream baos = new ByteArrayOutputStream(); 
		StreamUtil.copyStream(is, baos);
		String json = baos.toString();
		
		CompactJsonWriter w = new CompactJsonWriter();
		JSONParser jp = new JSONParser();
		JSONObject obj = (JSONObject) jp.parse(json);
		// ByteArrayOutputStream out1 = new ByteArrayOutputStream();
		FileOutputStream out1 = new FileOutputStream("c:\\temp\\out1.json");
		PrintWriter pw = new PrintWriter(out1);
		w.writeJSONValue(obj, pw);
		pw.flush();
		out1.close();
		
		
		PlanParser pp = new PlanParser(json, "1.2.3");
		
		// roundtrip attempt
		json = pp.toJson();
		System.out.println(json);
		obj = (JSONObject) jp.parse(json);
		FileOutputStream out2 = new FileOutputStream("c:\\temp\\out2.json");
		pw = new PrintWriter(out2);
		w.writeJSONValue(obj, pw);
		pw.flush();
		// System.out.println(out1.toString());
		out2.close();
		
		// diagram attempts
		Layout layout = new Layout(pp.topNode);
		Box b = layout.getLayoutBox();
		
		
		// @TODO translate diagram so that top-left is 0, 0
		RangeVisitor rv = new RangeVisitor();
		b.traverse(rv);
		// logger.info("range [" + rv.getMinX() + ", " + rv.getMinY() + "] - [" + rv.getMaxX() + ", " + rv.getMaxY() + "]");
		b.posX -= rv.getMinX();
		b.posY -= rv.getMinY();
		
		ReweightVisitor rwv = new ReweightVisitor(rv.minWeight, rv.maxWeight);
		b.traverse(rwv);

		
		FileOutputStream out3 = new FileOutputStream("c:\\temp\\out2.html");
		pw = new PrintWriter(out3);
		SvgBoxVisitor sbv = new SvgBoxVisitor(pw);
		b.traverse(sbv);
		pw.flush();
		out3.close();
		
		
		PrintWriter sysPw = new PrintWriter(System.out);
		sbv = new SvgBoxVisitor(sysPw);
		b.traverse(sbv);
		sysPw.flush();
	
		
	}
	
}
