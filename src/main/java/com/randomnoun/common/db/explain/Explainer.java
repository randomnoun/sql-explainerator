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
		protected abstract void writeHtml(PrintWriter pw);
	}
	
	public static class QueryBlockNode extends Node {
		public QueryBlockNode() {
			super("query_block", false);
		}
		@Override
		protected void writeHtml(PrintWriter pw) {
			// the things that a query block does
			pw.println("<table display=\"display: inline-table;\">"); // of course it is
			CostInfo ci = (CostInfo) this.attributes.get("costInfo");
			if (ci != null) {
				pw.println("<tr><td>Query cost: " + ci.getQueryCost() + "</td></tr>"); 
			}
			pw.println("<tr><td>[ query_block ]</td></tr>"); // tooltip = select ID & query cost
			pw.println("<tr><td>   ^</td></tr>");
			
			// the thing inside the query block
			if (this.children.size() > 0) {
				pw.println("<tr>");
				for (Node c : this.children) {
					pw.println("<td>");
					c.writeHtml(pw);
					pw.println("</td>");
				}
				pw.println("</tr>");
			}
			pw.println("</td></tr>");
			pw.println("</table>");
		}
	}
	
	public static class UnionResultNode extends Node {
		public UnionResultNode() {
			super("union_result", false); // TODO subtypes
		}
		protected void writeHtml(PrintWriter pw) {
			pw.println("<table display=\"display: inline-table;\">"); // of course it is
			pw.println("<tr><td style=\"background-color: #99999;\">UNION</td></tr>"); // tooltip: tableName, usingTemporaryTable: true
			pw.println("<tr><td>" + attributes.get("tableName") + "</td></tr>");
			QuerySpecificationsNode qsn = (QuerySpecificationsNode) this.children.get(0);
			if (qsn.children.size() > 0) {
				pw.println("<tr>");
				for (Node c : qsn.children) {
					pw.println("<td>");
					pw.println("<div>^</div>");
					c.writeHtml(pw);
					pw.println("</td>");
				}
				pw.println("</tr>");
			}
			pw.println("</td></tr>");
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
		public DuplicatesRemovalNode() {
			super("duplicates_removal", false); 
		}
		protected void writeHtml(PrintWriter pw) {
			pw.println("<table display=\"display: inline-table;\">"); // of course it is
			pw.println("<tr><td style=\"background-color: yellow;\">DISTINCT</td></tr>"); // tooltip: tableName, usingTemporaryTable: true
			pw.println("<tr><td>tmp table</td></tr>");
			if (this.children.size() > 0) {
				pw.println("<tr>");
				for (Node c : this.children) {
					pw.println("<td>");
					pw.println("<div>^</div>");
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
			super("nested_loop", true);
		}
		
		protected void writeHtml(PrintWriter pw) {
			pw.println("<table display=\"display: inline-table;\">"); // of course it is
			if (this.children.size() > 0) {
				pw.println("<tr>");
				for (Node c : this.children) {
					pw.println("<td>");
					pw.println("<div>nested loop</div>"); // tooltip: prefix cost
					pw.println("</td>");
				}
				pw.println("</tr>");
			}
			if (this.children.size() > 0) {
				pw.println("<tr>");
				for (Node c : this.children) {
					pw.println("<td>");
					pw.println("<div>^</div>");
					c.writeHtml(pw);
					pw.println("</td>");
				}
				pw.println("</tr>");
			}
			pw.println("</td></tr>");
			pw.println("</table>");
		}
	}
	
	public static class TableNode extends Node {
		public TableNode() {
			super("table", false);
		}
		
		protected void writeHtml(PrintWriter pw) {
			pw.println("<table display=\"display: inline-table;\">"); // of course it is
			pw.println("<tr><td style=\"background-color: red\">TABLE</td></tr>");
			pw.println("</table>");
			// @TODO attached subqueries
		}
	}
	
	public static class AttachedSubqueriesNode extends Node {
		public AttachedSubqueriesNode() {
			super("attached_subqueries", true);
		}
		
		protected void writeHtml(PrintWriter pw) {
			// @TODO this bit
		}
	}
	
	
	
	
	public static class ExplainDiagram {
		/* #
# JSON Explain Data Parser
#
# Takes a JSON object (bunch of dicts and lists) and turns into internal node objects
# */

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
			Node n = new QueryBlockNode();
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
			Node n = new UnionResultNode();
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
		
		private Node parseQuerySpecification(JSONObject obj) {
			Node n = new QuerySpecificationNode();
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
			Node n = new DuplicatesRemovalNode();
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
						Node cn = parseTable((JSONObject) cobj.get("table"));
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
		
		private Node parseTable(JSONObject obj) {
			Node n = new TableNode();
			n.attributes.put("tableName", obj.get("table_name"));
			n.attributes.put("distinct", obj.get("distinct")); // boolean
			n.attributes.put("accessType", obj.get("access_type")); // "ALL", "ref", "eq_ref"
			n.attributes.put("possibleKeys", parseNameList((JSONArray) obj.get("possible_keys"))); // List<String>
			if (obj.containsKey("key")) { n.attributes.put("key",  obj.get("key")); } // "PRIMARY"
			n.attributes.put("usedKeyParts", parseNameList((JSONArray) obj.get("used_key_parts"))); // List<String>, subset of above ?
			n.attributes.put("usedColumns", parseNameList((JSONArray) obj.get("used_columns"))); // List<String>
			if (obj.containsKey("key_length")) { n.attributes.put("keyLength", Long.parseLong((String) obj.get("key_length"))); }
			if (obj.containsKey("ref")) { n.attributes.put("ref", parseNameList((JSONArray) obj.get("ref"))); } // List<String>
			
			if (obj.containsKey("rows_examined_per_scan")) { n.attributes.put("rows_examined_per_scan", ((Number) obj.get("rows_examined_per_scan")).longValue()); }
			if (obj.containsKey("rows_produced_per_join")) { n.attributes.put("rows_produced_per_join", ((Number) obj.get("rows_produced_per_join")).longValue()); }
			if (obj.containsKey("filtered")) { n.attributes.put("filtered", Double.parseDouble((String) obj.get("filtered"))); }
			n.attributes.put("index_condition", obj.get("index_condition"));
			
			
			if (obj.containsKey("cost_info")) { n.attributes.put("costInfo", parseCostInfo((JSONObject) obj.get("cost_info"))); }
			n.attributes.put("attachedCondition", obj.get("attached_condition"));
			if (obj.containsKey("attached_subqueries")) {
				Node sn = new AttachedSubqueriesNode();
				n.addChild(sn);
				JSONArray qs = (JSONArray) obj.get("attached_subqueries");
				for (Object o : qs) {
					JSONObject cobj = (JSONObject) o;
					Node cn = parseQuerySpecification((JSONObject) o);
					sn.addChild(cn);
				}
				n.addChild(sn);
			}
			return n;
		}

		public String toJson() {
			
			return "{ \"" + Text.escapeJavascript(topNode.t) + "\": " + topNode.toJson() + "}";
		}

		public void writeHtml(PrintWriter pw) {
			pw.println("<html>");
			pw.println("<head><title>Here we go</title></head>");
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
		
		// roundtrip attempt
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
