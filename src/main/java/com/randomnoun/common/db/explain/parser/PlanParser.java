package com.randomnoun.common.db.explain.parser;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.randomnoun.common.Text;
import com.randomnoun.common.db.explain.enums.AccessTypeEnum;
import com.randomnoun.common.db.explain.json.AttachedSubqueriesNode;
import com.randomnoun.common.db.explain.json.CostInfoNode;
import com.randomnoun.common.db.explain.json.DuplicatesRemovalNode;
import com.randomnoun.common.db.explain.json.GroupingOperationNode;
import com.randomnoun.common.db.explain.json.ListNode;
import com.randomnoun.common.db.explain.json.MaterialisedFromSubqueryNode;
import com.randomnoun.common.db.explain.json.NameList;
import com.randomnoun.common.db.explain.json.NestedLoopNode;
import com.randomnoun.common.db.explain.json.Node;
import com.randomnoun.common.db.explain.json.OrderingOperationNode;
import com.randomnoun.common.db.explain.json.QueryBlockNode;
import com.randomnoun.common.db.explain.json.QuerySpecificationNode;
import com.randomnoun.common.db.explain.json.TableNode;
import com.randomnoun.common.db.explain.json.UnionResultNode;

/** Convert JSON to a hierarchy of Nodes */
public class PlanParser {

	private QueryBlockNode topNode;
	
	public PlanParser() {
	}
	
	public void parse(String json, String serverVersion) throws ParseException {
		JSONParser jp = new JSONParser();
		JSONObject obj = (JSONObject) jp.parse(json);
		
		QueryBlockNode n;
		if (obj.containsKey("query_block")) {
			n = parseQueryBlock((JSONObject) obj.get("query_block"));
		} else {
			throw new IllegalArgumentException("expected query_block");
		}
		this.topNode = n;
	}

	public void parse(Reader r, String serverVersion) throws ParseException, IOException {
		JSONParser jp = new JSONParser();
		JSONObject obj = (JSONObject) jp.parse(r);
		
		QueryBlockNode n;
		if (obj.containsKey("query_block")) {
			n = parseQueryBlock((JSONObject) obj.get("query_block"));
		} else {
			throw new IllegalArgumentException("expected query_block");
		}
		this.topNode = n;
	}

	// query_block
	private QueryBlockNode parseQueryBlock(JSONObject obj) {
		QueryBlockNode n = new QueryBlockNode();
		n.setSelectId(toLong(obj.get("select_id")));
		n.setMessage(obj.get("message"));
		
		n.getAttributes().put("selectId", obj.get("select_id"));
		n.getAttributes().put("message", obj.get("message"));
		if (obj.containsKey("cost_info")) {
			n.setCostInfo(parseCostInfo((JSONObject) obj.get("cost_info")));
			n.getAttributes().put("costInfo", n.getCostInfo()); 
		}
		
		// ok so my theory is that the qbn can contain exactly one of these things
		// which is always drawn underneath the query block rectangle
		
		if (obj.containsKey("union_result")) {
			Node cn = parseUnionResult((JSONObject) obj.get("union_result"));
			n.setQueryNode(cn);
			n.addChild(cn);
		} else if (obj.containsKey("duplicates_removal")) { // DISTINCT node
			Node cn = parseDuplicatesRemoval((JSONObject) obj.get("duplicates_removal"));
			n.setQueryNode(cn);
			n.addChild(cn);
			// other nodes
		} else if (obj.containsKey("table")) {
			Node cn = parseTable((JSONObject) obj.get("table"));
			n.setQueryNode(cn);
			n.addChild(cn);
		} else if (obj.containsKey("ordering_operation")) {
			Node cn = parseOrderingOperation((JSONObject) obj.get("ordering_operation"));
			n.setQueryNode(cn);
			n.addChild(cn);
		} else if (obj.containsKey("grouping_operation")) {
			Node cn = parseGroupingOperation((JSONObject) obj.get("grouping_operation"));
			n.setQueryNode(cn);
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

	private CostInfoNode parseCostInfo(JSONObject obj) {
		CostInfoNode ci = new CostInfoNode();
		if (obj.containsKey("query_cost")) { ci.setQueryCost(Double.parseDouble((String) obj.get("query_cost"))); }
		if (obj.containsKey("read_cost")) { ci.setReadCost(Double.parseDouble((String) obj.get("read_cost"))); }
		if (obj.containsKey("eval_cost")) { ci.setEvalCost(Double.parseDouble((String) obj.get("eval_cost"))); }
		if (obj.containsKey("prefix_cost")) { ci.setPrefixCost(Double.parseDouble((String) obj.get("prefix_cost"))); }
		if (obj.containsKey("sort_cost")) { ci.setSortCost(Double.parseDouble((String) obj.get("sort_cost"))); }
		ci.setDataReadPerJoin((String) obj.get("data_read_per_join"));
		return ci;
	}

	
	private UnionResultNode parseUnionResult(JSONObject obj) {
		UnionResultNode n = new UnionResultNode();
		n.setTableName((String) obj.get("table_name"));
		
		n.getAttributes().put("usingTemporaryTable", obj.get("using_temporary_table"));
		n.getAttributes().put("tableName", obj.get("table_name"));
		n.getAttributes().put("accessType", obj.get("access_type"));
		if (obj.containsKey("query_specifications")) {
			List<QuerySpecificationNode> qsnList = parseQuerySpecifications((JSONArray) obj.get("query_specifications"));
			n.setQuerySpecifications(qsnList);
			n.addChild(new ListNode("query_specifications", qsnList));
		} else {
			// other nodes
		}
		return n;
	}
	
	private List<QuerySpecificationNode> parseQuerySpecifications(JSONArray qs) {
		List<QuerySpecificationNode> qsnList = new ArrayList<>();
		for (Object o : qs) {
			JSONObject cobj = (JSONObject) o;
			QuerySpecificationNode cn = parseQuerySpecification((JSONObject) cobj);
			qsnList.add(cn);
		}
		return qsnList;
	}
	
	private QuerySpecificationNode parseQuerySpecification(JSONObject obj) {
		QuerySpecificationNode n = new QuerySpecificationNode();
		n.getAttributes().put("dependent", obj.get("dependent"));
		n.getAttributes().put("cacheable", obj.get("cacheable"));
		if (obj.containsKey("query_block")) {
			QueryBlockNode cn = parseQueryBlock((JSONObject) obj.get("query_block"));
			n.setQueryBlock(cn);
			n.addChild(cn);
		} else {
			throw new IllegalArgumentException("expected query_block in query_specification: " + obj.toString());
		}
		return n;
	}
	
	private MaterialisedFromSubqueryNode parseMaterialisedFromSubquery(JSONObject obj) {
		MaterialisedFromSubqueryNode n = new MaterialisedFromSubqueryNode();
		n.setDependent((Boolean) obj.get("dependent"));
		n.setCacheable((Boolean) obj.get("cacheable"));
		n.setUsingTemporaryTable((Boolean) obj.get("using_temporary_table"));
		
		n.getAttributes().put("dependent", obj.get("dependent"));
		n.getAttributes().put("cacheable", obj.get("cacheable"));
		n.getAttributes().put("usingTemporaryTable", obj.get("using_temporary_table")); // boolean
		if (obj.containsKey("query_block")) {
			QueryBlockNode cn = parseQueryBlock((JSONObject) obj.get("query_block"));
			n.setQueryBlock(cn);
			n.addChild(cn);
		} else {
			throw new IllegalArgumentException("expected query_block in query_specification: " + obj.toString());
		}
		
		return n;
	}
	 
			
	
	private DuplicatesRemovalNode parseDuplicatesRemoval(JSONObject obj) {
		DuplicatesRemovalNode n = new DuplicatesRemovalNode();
		n.setUsingTemporaryTable((Boolean) obj.get("using_temporary_table"));
				
		n.getAttributes().put("usingTemporaryTable", obj.get("using_temporary_table"));
		n.getAttributes().put("usingFilesort", obj.get("using_filesort"));
		// n.attributes.put("selectId", obj.get("select_id"));
		if (obj.containsKey("cost_info")) {
			n.setCostInfo(parseCostInfo((JSONObject) obj.get("cost_info")));
			n.getAttributes().put("costInfo", n.getCostInfo()); 
		}
		
		if (obj.containsKey("nested_loop")) {
			NestedLoopNode nln = parseNestedLoop((JSONArray) obj.get("nested_loop"));
			n.setNestedLoop(nln);
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
				n.getTables().add(cn);
				n.addChild(cn);
			} else {
				throw new IllegalArgumentException("expected table in nested_loop child");
			}
		}
		return n;
	}
	
	private OrderingOperationNode parseOrderingOperation(JSONObject obj) {
		OrderingOperationNode n = new OrderingOperationNode();
		if (obj.containsKey("using_temporary_table")) { n.setUsingTemporaryTable((Boolean) obj.get("using_temporary_table")); }
		n.setUsingFilesort((Boolean) obj.get("using_filesort"));
				
		n.getAttributes().put("usingTemporaryTable", obj.get("using_temporary_table"));
		n.getAttributes().put("usingFilesort", obj.get("using_filesort"));
		// n.attributes.put("selectId", obj.get("select_id"));
		// n.attributes.put("costInfo", parseCostInfo((JSONObject) obj.get("cost_info")));
		
		if (obj.containsKey("nested_loop")) {
			NestedLoopNode nln = parseNestedLoop((JSONArray) obj.get("nested_loop"));
			n.setOrderedNode(nln);
			n.addChild(nln);
		} else if (obj.containsKey("duplicates_removal")) { // DISTINCT node
			DuplicatesRemovalNode drn = parseDuplicatesRemoval((JSONObject) obj.get("duplicates_removal"));
			n.setOrderedNode(drn);
			n.addChild(drn);
		} else {
			throw new IllegalArgumentException("orderingOperation missing child");
		}

		return n;
	}
	
	private GroupingOperationNode parseGroupingOperation(JSONObject obj) {
		GroupingOperationNode n = new GroupingOperationNode();
		// n.usingTemporaryTable = (Boolean) obj.get("using_temporary_table");
		// n.attributes.put("usingTemporaryTable", obj.get("using_temporary_table"));
		
		if (obj.containsKey("using_filesort")) { n.setUsingFilesort((Boolean) obj.get("using_filesort")); }
		
		n.getAttributes().put("usingFilesort", obj.get("using_filesort"));
		// n.attributes.put("selectId", obj.get("select_id"));
		// n.attributes.put("costInfo", parseCostInfo((JSONObject) obj.get("cost_info")));
		
		if (obj.containsKey("nested_loop")) {
			NestedLoopNode nln = parseNestedLoop((JSONArray) obj.get("nested_loop"));
			n.setNestedLoop(nln);
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
		n.setTableName((String) obj.get("table_name"));
		n.getAttributes().put("tableName", obj.get("table_name"));
		
		
		n.getAttributes().put("distinct", obj.get("distinct")); // boolean
		n.getAttributes().put("usingIndex", obj.get("using_index")); // boolean
		
		n.setAccessType(AccessTypeEnum.fromJsonValue((String) obj.get("access_type")));
		n.getAttributes().put("accessType", obj.get("access_type")); // "ALL", "ref", "eq_ref"
		
		List<String> possibleKeys = parseNameList((JSONArray) obj.get("possible_keys"));
		n.setPossibleKeys(possibleKeys);
		n.getAttributes().put("possibleKeys", possibleKeys); // List<String>
		
		if (obj.containsKey("key")) { n.setKey((String) obj.get("key")); n.getAttributes().put("key",  obj.get("key")); } // "PRIMARY"
		
		List<String> usedKeyParts = parseNameList((JSONArray) obj.get("used_key_parts"));
		n.setUsedKeyParts(usedKeyParts);
		n.getAttributes().put("usedKeyParts", usedKeyParts); // List<String>, subset of above ?
		
		List<String> usedColumns = parseNameList((JSONArray) obj.get("used_columns"));
		n.setUsedColumns(usedColumns);
		n.getAttributes().put("usedColumns", usedColumns); // List<String>
		if (obj.containsKey("key_length")) { n.getAttributes().put("keyLength", Long.parseLong((String) obj.get("key_length"))); }
		if (obj.containsKey("ref")) { n.getAttributes().put("ref", parseNameList((JSONArray) obj.get("ref"))); } // List<String>
		
		if (obj.containsKey("rows_examined_per_scan")) { 
			n.setRowsExaminedPerScan(((Number) obj.get("rows_examined_per_scan")).longValue());
			n.getAttributes().put("rows_examined_per_scan", n.getRowsExaminedPerScan()); 
			}
		if (obj.containsKey("rows_produced_per_join")) { 
			n.setRowsProducedPerJoin(((Number) obj.get("rows_produced_per_join")).longValue()); 
			n.getAttributes().put("rows_produced_per_join", n.getRowsProducedPerJoin()); 
			}
		if (obj.containsKey("filtered")) {
			Double filtered = Double.parseDouble((String) obj.get("filtered"));
			n.setFiltered(filtered);
			n.getAttributes().put("filtered", filtered); 
		}
		n.getAttributes().put("index_condition", obj.get("index_condition"));
		
		if (obj.containsKey("cost_info")) { 
			n.setCostInfo(parseCostInfo((JSONObject) obj.get("cost_info"))); 
			n.getAttributes().put("costInfo", n.getCostInfo()); 
		}

		// @TODO object form ?
		n.getAttributes().put("attachedCondition", obj.get("attached_condition"));
		
		if (obj.containsKey("attached_subqueries")) {
			AttachedSubqueriesNode sn = new AttachedSubqueriesNode();
			n.setAttachedSubqueries(sn);
			n.addChild(sn);
			JSONArray qs = (JSONArray) obj.get("attached_subqueries");
			for (Object o : qs) {
				JSONObject cobj = (JSONObject) o;
				QuerySpecificationNode qsn = parseQuerySpecification((JSONObject) cobj);
				sn.setQuerySpecification(qsn);
				sn.addChild(qsn);
			}
		}
		
		// can we have attached_subqueries and a materialised view at the same time ?
		
		if (obj.containsKey("materialized_from_subquery")) {
			MaterialisedFromSubqueryNode sn = parseMaterialisedFromSubquery((JSONObject) obj.get("materialized_from_subquery"));
			n.setMaterialisedFromSubquery(sn);
			n.addChild(sn);
		}
		
		
		return n;
	}

	public String toJson() {
		
		return "{ \"" + Text.escapeJavascript(topNode.getJsonType()) + "\": " + topNode.toJson() + "}";
	}

	public QueryBlockNode getTopNode() {
		return topNode;
	}

	public void setTopNode(QueryBlockNode topNode) {
		this.topNode = topNode;
	}

}