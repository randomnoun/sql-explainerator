package com.randomnoun.common.db.explain;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
 */

public class Explainer {

	static Logger logger = Logger.getLogger(Explainer.class);
	
	private static enum AccessTypeEnum {
		FULL_TABLE_SCAN("ALL"),
		NON_UNIQUE_KEY("ref"),
		UNIQUE_KEY("eq_ref");
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
		Double queryCost;
		Double readCost;
		Double evalCost;
		Double prefixCost;
		String dataReadPerJoin; // "96K"
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
	
	
	public static abstract class Node implements Struct.ToJson {
		private String t;
		private boolean isArray;
		protected Map<String, Object> attributes = new HashMap<>();
		protected List<Node> children = new ArrayList<>();
		
		public Node(String t, boolean isArray) {
			this.t = t;
			this.isArray = isArray;
		}
		public void addChild(Node c) {
			children.add(c);
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
						"{ \"" + Text.escapeJavascript(c.t) + "\": " + c.toJson() + "}";
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
						s += (f == true ? "": ", ") + "\"" + Text.escapeJavascript(n.t) + "\": " + n.toJson(); // -CHILD 
					} else {
						remaining.add(v);
					}
				}
				if (remaining.size() > 0) {
					throw new IllegalStateException("didn't expect remaining children here");
					/*
					s += (f == true ? "": ", ") + "\"children\": [";
					for (int i = 0; i < remaining.size(); i++) {
						s += (i==0 ? "" : ", ") + children.get(i).toJson();
					}
					s += "]";
					*/
				}
				s += "}";
			}
			return s;
		}
		
		public long getHtmlBlockWidth() {
			// assume each child is side-by-side, override when it isn't
			long childBlockWidth = children.stream()
				.reduce((long) 0, (w, c) -> { return w + c.getHtmlBlockWidth(); }, Long::sum);
			return Math.max(1, childBlockWidth);
		}
		
		public long getPixelWidth() {
			// assume each child is side-by-side, override when it isn't
			long childPixelWidth = children.stream()
				.reduce((long) 0, (w, c) -> { return w + c.getPixelWidth(); }, Long::sum);
			return Math.max(180, childPixelWidth);
		}
		
		public long getLeftPaddingPixelWidth() {
			// left padding on the first TD in the html for this node
			// think this is probably over-engineering to the particular query I'm looking at right now
			// override for nodes that don't have entry branches from the middle of the TD
			long leftPaddingPixelWidth = children.stream()
					.reduce((long) 0, (w, c) -> { return w + c.getPixelWidth(); }, Long::sum);
			return Math.max(0, leftPaddingPixelWidth);
		}
		
		protected abstract void writeHtml(PrintWriter pw);
		
		protected Stream<Node> reverseStream(List<Node> children) {
			// why the hell isn't this on https://stackoverflow.com/questions/24010109/java-8-stream-reverse-order 
			// ?
			return IntStream.range(0, children.size())
				.mapToObj(i -> children.get(children.size() - i - 1)); 
		}

	}
	
	public static class QueryBlockNode extends Node {
		
		public Object selectId;
		public Object message;
		
		public QueryBlockNode() {
			super("query_block", false);
		}

		@Override
		public long getLeftPaddingPixelWidth() {
			// so the theory is that I don't need to extend padding across query blocks
			return 0;
		}

		
		
		@Override
		protected void writeHtml(PrintWriter pw) {
			pw.println("<table display=\"display: inline-table;\">"); // of course it is
			CostInfo ci = (CostInfo) this.attributes.get("costInfo");
			if (ci != null) {
				pw.println("<tr><td><div class=\"queryCost\">Query cost: " + ci.getQueryCost() + "</div></td></tr>"); 
			}
			String label = "query_block" + (selectId == null ? "" : " #" + selectId);
			String clazz = (selectId == null ? " topNode" : "");
			long lp = super.getLeftPaddingPixelWidth();
			String leftPad = lp == 0 ? "" : " style=\"padding-left: " + lp + ";\"";
			
			pw.println("<tr><td" + leftPad + "><div class=\"queryBlock " + clazz + "\">" + label + "</div></td></tr>"); // tooltip = select ID & query cost
			if (message != null) {
				pw.println("<tr><td" + leftPad + ">" + message + "</td></tr>");
			}
			// the thing inside the query block
			if (this.children.size() > 0) {
				pw.println("<tr><td" + leftPad + "><div class=\"upArrow\"></div></td></tr>");
				pw.println("<tr>");
				reverseStream(children).forEach( c -> {
					pw.println("<td>");
					c.writeHtml(pw);
					pw.println("</td>");
				});
				pw.println("</tr>");
			}
			pw.println("</td></tr>");
			pw.println("</table>");
		}
	}
	
	public static class UnionResultNode extends Node {
		public String tableName;
		public UnionResultNode() {
			super("union_result", false); // TODO subtypes
		}
		protected void writeHtml(PrintWriter pw) {
			QuerySpecificationsNode qsn = (QuerySpecificationsNode) this.children.get(0);
			
			pw.println("<table display=\"display: inline-table;\">"); // of course it is
			String colspan = " colspan=\"" + Math.max(1, qsn.children.size()) + "\"";
			pw.println("<tr><td " + colspan + "><div class=\"union\">UNION</div></td></tr>"); // tooltip: tableName, usingTemporaryTable: true
			pw.println("<tr><td " + colspan + "><div class=\"tableName\">" + Text.escapeHtml(tableName) + "</div></td></tr>");
			if (qsn.children.size() > 0) {
				pw.println("<tr>");
				// for (Node c : qsn.children) {
				reverseStream(qsn.children).forEach( c -> {
					pw.println("<td>");
					pw.println("<div class=\"upArrow\"></div>");
					c.writeHtml(pw);
					pw.println("</td>");
				});
				pw.println("</tr>");
			}
			// pw.println("</td></tr>");
			pw.println("</table>");
			
		}
	}
	
	// collection node
	public static class QuerySpecificationsNode extends Node {
		public QuerySpecificationsNode() {
			super("query_specifications", true);
		}
		protected void writeHtml(PrintWriter pw) {
			throw new IllegalStateException("didn't expect that");
		}
	}
	
	public static class QuerySpecificationNode extends Node {
		public QuerySpecificationNode() {
			super("query_specification", false);
		}
		protected void writeHtml(PrintWriter pw) {
			// attributes of this node don't appear in the diagram
			QueryBlockNode qbn = (QueryBlockNode) children.get(0);
			qbn.writeHtml(pw);
		}
	}
	
	public static class DuplicatesRemovalNode extends Node {
		public Boolean usingTemporaryTable;
		public DuplicatesRemovalNode() {
			super("duplicates_removal", false); 
		}
		protected void writeHtml(PrintWriter pw) {
			pw.println("<table display=\"display: inline-table;\">"); // of course it is
			pw.println("<tr><td><div class=\"duplicatesRemoval\">DISTINCT</div></td></tr>"); // tooltip: tableName, usingTemporaryTable: true
			if (usingTemporaryTable) {
				pw.println("<tr><td><div class=\"tempTableName\">tmp table</div></td></tr>"); // tooltip: tableName, usingTemporaryTable: true
			}
			
			// pw.println("<tr><td>tmp table</td></tr>");
			if (this.children.size() > 0) {
				// just has a single NestedLoop child
				pw.println("<tr>");
				for (Node c : this.children) {
					pw.println("<td>");
					pw.println("<div class=\"upArrow\"></div>");
					c.writeHtml(pw);
					pw.println("</td>");
				}
				pw.println("</tr>");
			}
			pw.println("</td></tr>");
			pw.println("</table>");
		}
	}
	
	public static class NestedLoopNode extends Node {
		public NestedLoopNode() {
			super("nestedLoop", true);
		}
		
		// probably need a getWidth() on nodes
		// so that we can align things above them properly
		
		public long getLeftPaddingPixelWidth() {
			// pixel width of child nodes 0 to n-1
			int leftPad = 0; // should be getting this from npm really. boom boom.
			for (int i = 0; i < this.children.size() - 1; i++) {
				leftPad += this.children.get(i).getPixelWidth();
			}
			return leftPad;
		}
		
		protected void writeHtml(PrintWriter pw) {
			pw.println("<table display=\"display: inline-table;\">"); // of course it is
			
			// OK so my theory is that the 'prefixCost' is a cumulative cost of all the costs leading up to this node ( before = prefix )
			
			// on mysql workbench, the cost value leading out of the nestedLoop is the prefixCost of the last node
			// and the row count leading out of the nestedLoop is the rowsProducedPerJoin of the last node
			
			// and the values above each table node is the evalCost + readCost of that table
			
			
			if (this.children.size() > 0) {
				for (int i = 0; i < this.children.size(); i++) {
					TableNode c = (TableNode) this.children.get(i);
					if (i == 0) {
						pw.println("<td>");
						pw.println("</td>");
						pw.println("<td></td>"); // gap for right arrow
						
					} else {
						pw.println("<td><div class=\"queryCost\">");
						pw.println(c.costInfo==null || c.costInfo.prefixCost == null ? "" : c.costInfo.prefixCost);
						pw.println("</div>");
						if (i == this.children.size() - 1) {
							pw.println("<div class=\"rhsQueryCost\">" +
								(c.rowsProducedPerJoin == null ? "" : c.rowsProducedPerJoin + 
									(c.rowsProducedPerJoin == 1 ? " row" : " rows")) +
								"</div>"); // gap for right arrow
						}						
						pw.println("</td>");
						if (i < this.children.size() - 1) {
							pw.println("<td></td>"); // gap for right arrow
						}
					}
				}
				pw.println("</tr>");

				
				
				pw.println("<tr>");
				for (int i = 0; i < this.children.size(); i++) {
					TableNode c = (TableNode) this.children.get(i);
					if (i == 0) {
						pw.println("<td>");
						pw.println("<svg width=\"60\" height=\"60\"></svg>");
						pw.println("</td>");
						pw.println("<td class=\"rightArrow\"><div class=\"rightArrow\"></div>");
						pw.println("</td>");
						
					} else {
						pw.println("<td>");
						// probably going to have to use SVG here aren't I
						// holy crap I can't believe that almost worked
						pw.println("<svg width=\"60\" height=\"60\"><g><path fill=\"none\" stroke=\"black\" d=\"M 30,0 L 60 30 L 30 60 L 0 30 L 30 0\"></path>");
						pw.println("<text x=\"30\" y=\"25\" font-size=\"10px\" dominant-baseline=\"middle\" text-anchor=\"middle\">nested</text>");
						pw.println("<text x=\"30\" y=\"35\" font-size=\"10px\" dominant-baseline=\"middle\" text-anchor=\"middle\">loop</text>");
						pw.println("</g></svg>");
						// pw.println("<div>nested loop</div>"); // tooltip: prefix cost
						pw.println("</td>");
						if (i < this.children.size() - 1) {
							pw.println("<td class=\"rightArrow\">");
							pw.println("<div class=\"nestedLoopRightArrowLabel\">" + (c.rowsProducedPerJoin == null ? "" :
								c.rowsProducedPerJoin + (c.rowsProducedPerJoin == 1 ? " row" : " rows")) + "</div>");
							pw.println("<div class=\"rightArrow\"></div>");
							pw.println("</td>");
						}
					}
				}
				pw.println("</tr>");
			}
			if (this.children.size() > 0) {
				pw.println("<tr>");
				for (Node n : this.children) {
					TableNode c = (TableNode) n;
					pw.println("<td>");
					pw.println("<div><div class=\"upArrow\"></div>");
					c.writeHtml(pw);
					pw.println("</td>");
					if (c.attachedSubqueries == null) {
						pw.println("<td>"); // gap for right arrow
						pw.println("</td>");
					} else {
						// that bit.
						pw.println("<td>"); // gap for right arrow
						c.attachedSubqueries.writeHtml(pw);
						pw.println("</td>");
					}
				}
				pw.println("</tr>");
			}
			pw.println("</td></tr>");
			pw.println("</table>");
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

		public TableNode() {
			super("table", false);
		}
		
		protected void writeHtml(PrintWriter pw) {
			pw.println("<table display=\"display: inline-table;\">"); // of course it is
			pw.println("<tr><td><div class=\"lhsQueryCost\">");
			pw.println(costInfo==null ? "" : (costInfo.evalCost == null ? (double) 0 : costInfo.evalCost) +
					(costInfo.readCost == null ? (double) 0 : costInfo.readCost));
			pw.println("</div><div class=\"rhsQueryCost\">");
			pw.println(rowsExaminedPerScan == null ? "" : String.valueOf(rowsExaminedPerScan) + 
					(rowsExaminedPerScan == 1 ? " row" : "rows")); 
			pw.println("</div>");
			
			pw.println("</td></tr>");
			
			pw.println("<tr><td><div class=\"table " + 
				(accessType==AccessTypeEnum.FULL_TABLE_SCAN ? " fullTableScan" :
				(accessType==AccessTypeEnum.NON_UNIQUE_KEY ? " nonUniqueKey" :
				(accessType==AccessTypeEnum.UNIQUE_KEY ? " uniqueKey" : ""))) + "\">");
			pw.println(
				(accessType==AccessTypeEnum.FULL_TABLE_SCAN ? "Full Table Scan" :
				(accessType==AccessTypeEnum.NON_UNIQUE_KEY ? " Non-Unique Key Lookup" :
				(accessType==AccessTypeEnum.UNIQUE_KEY ? "Unique Key Lookup" : ""))));
			pw.println("</div></td></tr>");
			
			pw.println("<tr><td><div class=\"tableName\">");
			pw.println(tableName);
			pw.println("</td></tr>");

			pw.println("<tr><td><div class=\"tableKey\">");
			pw.println(key == null ? "" : key);
			pw.println("</td></tr>");

			pw.println("</table>");
			// @TODO attached subqueries
		}
	}
	
	public static class AttachedSubqueriesNode extends Node {
		public QuerySpecificationNode querySpecification;

		public AttachedSubqueriesNode() {
			super("attached_subqueries", true);
		}
		
		protected void writeHtml(PrintWriter pw) {
			pw.println("<table display=\"display: inline-table;\">"); // of course it is
			pw.println("<tr><td><div class=\"leftArrow\"></div></td>");
			pw.println("    <td><div class=\"attachedSubqueries\">attached_subqueries</div></td>");
			pw.println("</tr>");
			pw.println("<tr><td></td>"); // gap for leftArrow
			pw.println("    <td><div class=\"upArrow\"></div></td>");
			pw.println("</tr>");
			pw.println("<tr><td></td>");
			pw.println("    <td>");
			querySpecification.writeHtml(pw);
			// the attached subquery
			pw.println("    </td>");
			pw.println("</tr>");
			
			pw.println("</table>");
			
		}
	}
	

	
	
	
	public static class ExplainDiagram {

		private Node topNode;
		String json;
		
		public ExplainDiagram(String json, String server_version) throws ParseException {
			this.json = json;
			this.topNode = getNode(json);
		}
		
		public Node getNode(String json) throws ParseException {
			JSONParser jp = new JSONParser();
			JSONObject obj = (JSONObject) jp.parse(json);
			
			// ok then
			Node n;
			if (obj.containsKey("query_block")) {
				n = parseQueryBlock((JSONObject) obj.get("query_block"));
			} else {
				throw new IllegalArgumentException("expected query_block");
			}
			return n;
		}

		// query_block
		private Node parseQueryBlock(JSONObject obj) {
			QueryBlockNode n = new QueryBlockNode();
			n.selectId = toLong(obj.get("select_id"));
			n.message = obj.get("message");
			
			n.attributes.put("selectId", obj.get("select_id"));
			n.attributes.put("message", obj.get("message"));
			if (obj.containsKey("cost_info")) { n.attributes.put("costInfo", parseCostInfo((JSONObject) obj.get("cost_info"))); }
			
			if (obj.containsKey("union_result")) {
				Node cn = parseUnionResult((JSONObject) obj.get("union_result"));
				n.addChild(cn);
			} else if (obj.containsKey("duplicates_removal")) { // DISTINCT node
				Node cn = parseDuplicatesRemoval((JSONObject) obj.get("duplicates_removal"));
				n.addChild(cn);
				// other nodes
			} else if (obj.containsKey("table")) {
				Node cn = parseTable((JSONObject) obj.get("table"));
				n.addChild(cn);
			}
			return n;
		}
		
		private Object toLong(Object object) {
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

		
		private Node parseUnionResult(JSONObject obj) {
			UnionResultNode n = new UnionResultNode();
			n.tableName = (String) obj.get("table_name");
			
			n.attributes.put("usingTemporaryTable", obj.get("using_temporary_table"));
			n.attributes.put("tableName", obj.get("table_name"));
			n.attributes.put("accessType", obj.get("access_type"));
			if (obj.containsKey("query_specifications")) {
				Node sn = new QuerySpecificationsNode();
				n.addChild(sn);
				JSONArray qs = (JSONArray) obj.get("query_specifications");
				for (Object o : qs) {
					JSONObject cobj = (JSONObject) o;
					Node cn = parseQuerySpecification((JSONObject) o);
					sn.addChild(cn);
				}
			} else {
				// other nodes
			}
			return n;
		}
		
		private QuerySpecificationNode parseQuerySpecification(JSONObject obj) {
			QuerySpecificationNode n = new QuerySpecificationNode();
			n.attributes.put("dependent", obj.get("dependent"));
			n.attributes.put("cacheable", obj.get("cacheable"));
			if (obj.containsKey("query_block")) {
				Node cn = parseQueryBlock((JSONObject) obj.get("query_block"));
				n.addChild(cn);
			} else {
				throw new IllegalArgumentException("expected query_block in query_specification: " + obj.toString());
			}
			return n;
		}
		
		private Node parseDuplicatesRemoval(JSONObject obj) {
			DuplicatesRemovalNode n = new DuplicatesRemovalNode();
			n.usingTemporaryTable = (Boolean) obj.get("using_temporary_table");
					
			n.attributes.put("usingTemporaryTable", obj.get("using_temporary_table"));
			n.attributes.put("usingFilesort", obj.get("using_filesort"));
			// n.attributes.put("selectId", obj.get("select_id"));
			// n.attributes.put("costInfo", parseCostInfo((JSONObject) obj.get("cost_info")));
			
			if (obj.containsKey("nested_loop")) {
				Node sn = new NestedLoopNode();
				n.addChild(sn);
				JSONArray loopItems = (JSONArray) obj.get("nested_loop");
				for (Object o : loopItems) {
					JSONObject cobj = (JSONObject) o;
					if (cobj.containsKey("table")) {
						TableNode cn = parseTable((JSONObject) cobj.get("table"));
						sn.addChild(cn);
					} else {
						throw new IllegalArgumentException("expected table in nested_loop child");
					}
				}
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
			n.tableName = (String) obj.get("table_name");
			n.attributes.put("tableName", obj.get("table_name"));
			n.attributes.put("distinct", obj.get("distinct")); // boolean
			
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
			
			
			if (obj.containsKey("cost_info")) { n.costInfo = parseCostInfo((JSONObject) obj.get("cost_info")); n.attributes.put("costInfo", n.costInfo); }
			n.attributes.put("attachedCondition", obj.get("attached_condition"));
			if (obj.containsKey("attached_subqueries")) {
				AttachedSubqueriesNode sn = new AttachedSubqueriesNode();
				n.attachedSubqueries = sn;
				n.addChild(sn);
				JSONArray qs = (JSONArray) obj.get("attached_subqueries");
				for (Object o : qs) {
					JSONObject cobj = (JSONObject) o;
					QuerySpecificationNode qsn = parseQuerySpecification((JSONObject) o);
					sn.querySpecification = qsn;
					sn.addChild(qsn);
				}
			}
			return n;
		}

		public String toJson() {
			
			return "{ \"" + Text.escapeJavascript(topNode.t) + "\": " + topNode.toJson() + "}";
		}

		public void writeHtml(PrintWriter pw) {
			pw.println("<html>");
			pw.println("<head><title>Here we go</title>");
			pw.println("<style>");
			
			InputStream is = Explainer.class.getResourceAsStream("/css.css");
			String s;
			try {
				s = new String(StreamUtil.getByteArray(is));
			} catch (IOException e) {
				throw new IllegalStateException("IOException", e);
			}
			pw.println(s);
			
			// pw.println("TD { vertical-align: top; }");
			pw.println("</style>");
			pw.println("</head>");
			
			pw.println("<body>");
			topNode.writeHtml(pw);
			
			pw.println("</body></html>");
			
			
		}
		
	}
	
	
	public Explainer() {
		
	}
	
	public void explain() {
		
	}
	
	public static void main(String args[]) throws IOException, ParseException {
		Log4jCliConfiguration lcc = new Log4jCliConfiguration();
		lcc.init("[explain-to-image]", null);
		
		InputStream is = Explainer.class.getResourceAsStream("/somequery.json");
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
		
		
		ExplainDiagram ec = new ExplainDiagram(json, "1.2.3");
		
		// roundtrip attemptt
		json = ec.toJson();
		System.out.println(json);
		obj = (JSONObject) jp.parse(json);
		FileOutputStream out2 = new FileOutputStream("c:\\temp\\out2.json");
		pw = new PrintWriter(out2);
		w.writeJSONValue(obj, pw);
		pw.flush();
		// System.out.println(out1.toString());
		out2.close();
		

		
		FileOutputStream outHtml = new FileOutputStream("c:\\temp\\out2.html");
		pw = new PrintWriter(outHtml);
		ec.writeHtml(pw);
		pw.flush();
		// System.out.println(out1.toString());
		out2.close();

		
		
		// @TODO roundtrip it and see what broke
		
		
		
		
	}
	
}
