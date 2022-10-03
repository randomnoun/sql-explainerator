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
import com.randomnoun.common.db.explain.json.BufferResultNode;
import com.randomnoun.common.db.explain.json.CostInfoNode;
import com.randomnoun.common.db.explain.json.DuplicatesRemovalNode;
import com.randomnoun.common.db.explain.json.GroupingOperationNode;
import com.randomnoun.common.db.explain.json.HavingSubqueriesNode;
import com.randomnoun.common.db.explain.json.InsertFromNode;
import com.randomnoun.common.db.explain.json.ListNode;
import com.randomnoun.common.db.explain.json.MaterialisedFromSubqueryNode;
import com.randomnoun.common.db.explain.json.NameList;
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

/** Convert JSON to a hierarchy of Nodes */
public class PlanParser {

	private QueryBlockNode topNode;
	String serverVersion;
	boolean newLayout = false;
	
	public PlanParser() {
		// default parser
	}

	public PlanParser(String serverVersion, boolean newLayout) {
		this.serverVersion = serverVersion;
		this.newLayout = newLayout;
	}

	
	public void parse(String json) throws ParseException {
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

	public void parse(Reader r) throws ParseException, IOException {
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
		
		Node cn = parseQueryNode(obj);
		if (cn != null) {
			n.setQueryNode(cn);
			n.addChild(cn);
		}
		if (obj.containsKey("insert_from")) {
			n.setInsertFromNode(parseInsertFromNode((JSONObject) obj.get("insert_from")));
			n.getAttributes().put("insert_from",  n.getInsertFromNode());
		}
		
		return n;
	}
	
	private InsertFromNode parseInsertFromNode(JSONObject obj) {
		InsertFromNode ifn = new InsertFromNode();
		Node qn = parseQueryNode(obj);
		ifn.setQueryNode(qn);
		ifn.addChild(qn);
		return ifn;
	}
	
	private Node parseQueryNode(JSONObject obj) {
		Node cn = null;
		if (obj.containsKey("union_result")) {
			cn = parseUnionResult((JSONObject) obj.get("union_result"));
		} else if (obj.containsKey("duplicates_removal")) { // DISTINCT node
			cn = parseDuplicatesRemoval((JSONObject) obj.get("duplicates_removal"));
		} else if (obj.containsKey("table")) {
			cn = parseTable((JSONObject) obj.get("table"));
		} else if (obj.containsKey("ordering_operation")) {
			cn = parseOrderingOperation((JSONObject) obj.get("ordering_operation"));
		} else if (obj.containsKey("grouping_operation")) {
			cn = parseGroupingOperation((JSONObject) obj.get("grouping_operation"));
		} else if (obj.containsKey("nested_loop")) {
			cn = parseNestedLoop((JSONArray) obj.get("nested_loop"));
		} else if (obj.containsKey("windowing") && newLayout) {
			cn = parseWindowing((JSONObject) obj.get("windowing"));
		} else if (obj.containsKey("buffer_result") && newLayout) {
			cn = parseBufferResult((JSONObject) obj.get("buffer_result"));
			
		} else {
			// can have 'no tables used' if this is a SELECT without a table
			// throw new IllegalStateException("Expected 'union_result', 'duplicates_removal', 'table', 'ordering_operation', 'grouping_operation' in query block " + n.selectId);
		}
		return cn;
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
	
	private BufferResultNode parseBufferResult(JSONObject obj) {
		BufferResultNode brn = new BufferResultNode();
		QueryBlockNode cn = parseQueryBlock(obj);
		brn.setQueryBlock(cn);
		brn.addChild(cn);
		return brn;
	}
	
	private List<QuerySpecificationNode> parseQuerySpecifications(JSONArray qs) {
		List<QuerySpecificationNode> qsnList = new ArrayList<>();
		for (Object o : qs) {
			JSONObject cobj = (JSONObject) o;
			QuerySpecificationNode cn = parseQuerySpecification(cobj);
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
		} else if (obj.containsKey("table")) {
			// this object is a table, which is also a queryBlock. hrm.
			QueryBlockNode cn = parseQueryBlock(obj);
			n.setQueryBlock(cn);
			n.addChild(cn);

		} else {
			throw new IllegalArgumentException("expected query_block in query_specification: " + obj.toString());
		}
		return n;
	}
	
	private MaterialisedFromSubqueryNode parseMaterialisedFromSubquery(JSONObject obj) {
		MaterialisedFromSubqueryNode n = new MaterialisedFromSubqueryNode();
		if (obj.containsKey("dependent")) {
			n.setDependent((Boolean) obj.get("dependent"));
		}
		if (obj.containsKey("cacheable")) {
			n.setCacheable((Boolean) obj.get("cacheable"));
		}
		if (obj.containsKey("using_temporary_table")) {
			n.setUsingTemporaryTable((Boolean) obj.get("using_temporary_table"));
		}
		
		n.getAttributes().put("dependent", obj.get("dependent"));
		n.getAttributes().put("cacheable", obj.get("cacheable"));
		n.getAttributes().put("usingTemporaryTable", obj.get("using_temporary_table")); // boolean
		if (obj.containsKey("query_block")) {
			QueryBlockNode cn = parseQueryBlock((JSONObject) obj.get("query_block"));
			n.setSubquery(cn);
			n.addChild(cn);
		} else if (obj.containsKey("sharing_temporary_table_with") && newLayout) {
			SharingTemporaryTableWithNode sttwn = parseSharingTemporaryTableWithNode((JSONObject) obj.get("sharing_temporary_table_with"));
			n.setSubquery(sttwn);
			n.addChild(sttwn);
			
		} else if (newLayout) {
			throw new IllegalArgumentException("expected query_block in query_specification: " + obj.toString());
		}
		
		return n;
	}
	 
	private SharingTemporaryTableWithNode parseSharingTemporaryTableWithNode(JSONObject obj) {
		SharingTemporaryTableWithNode n = new SharingTemporaryTableWithNode();
		n.setSelectId(toLong(obj.get("select_id")));
		n.getAttributes().put("selectId", n.getSelectId());
		return n;
	}
	
	private DuplicatesRemovalNode parseDuplicatesRemoval(JSONObject obj) {
		DuplicatesRemovalNode n = new DuplicatesRemovalNode();
		if (obj.containsKey("using_temporary_table") ) {
			n.setUsingTemporaryTable((Boolean) obj.get("using_temporary_table"));
		}
		n.setUsingFilesort((Boolean) obj.get("using_filesort"));
				
		n.getAttributes().put("usingTemporaryTable", obj.get("using_temporary_table"));
		n.getAttributes().put("usingFilesort", obj.get("using_filesort"));
		if (obj.containsKey("cost_info")) {
			n.setCostInfo(parseCostInfo((JSONObject) obj.get("cost_info")));
			n.getAttributes().put("costInfo", n.getCostInfo()); 
		}
		Node cn = parseQueryNode(obj);
		if (cn == null) {
			throw new IllegalArgumentException("expected duplicates_removal child");
		} else {
			n.setChildNode(cn);
			n.addChild(cn);
		}
		return n;
	}
	
	private NestedLoopNode parseNestedLoop(JSONArray loopItems) {
		NestedLoopNode n = new NestedLoopNode();
		
		for (Object o : loopItems) {
			JSONObject cobj = (JSONObject) o;
			if (cobj.containsKey("table")) {
				TableNode cn = parseTable((JSONObject) cobj.get("table"));
				n.addTable(cn);
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
		// n.attributes.put("costInfo", parseCostInfo((JSONObject) obj.get("cost_info")));
		
		Node cn = parseQueryNode(obj);
		if (cn == null && newLayout) {
			throw new IllegalArgumentException("expected ordering_operation child");
		} else {
			n.setOrderedNode(cn);
			n.addChild(cn);
		}

		return n;
	}
	
	private GroupingOperationNode parseGroupingOperation(JSONObject obj) {
		GroupingOperationNode n = new GroupingOperationNode();
		if (obj.containsKey("using_temporary_table")) { n.setUsingTemporaryTable((Boolean) obj.get("using_temporary_table")); }
		n.setUsingFilesort((Boolean) obj.get("using_filesort"));
		
		if (obj.containsKey("using_filesort")) { n.setUsingFilesort((Boolean) obj.get("using_filesort")); }
		
		n.getAttributes().put("usingFilesort", obj.get("using_filesort"));
		// n.attributes.put("costInfo", parseCostInfo((JSONObject) obj.get("cost_info")));

		Node cn = parseQueryNode(obj);
		if (cn == null) {
			throw new IllegalArgumentException("expected grouping_operation child");
		} else {
			n.setGroupedNode(cn);
			n.addChild(cn);
		}
		
		if (obj.containsKey("having_subqueries") && newLayout) {
			HavingSubqueriesNode sn = new HavingSubqueriesNode();
			n.setHavingSubqueries(sn);
			n.addChild(sn);
			JSONArray qs = (JSONArray) obj.get("having_subqueries");
			for (Object o : qs) {
				JSONObject cobj = (JSONObject) o;
				QuerySpecificationNode qsn = parseQuerySpecification(cobj);
				sn.addQuerySpecification(qsn);
				sn.addChild(qsn);
			}
		}

		return n;
	}
	
	private WindowingNode parseWindowing(JSONObject obj) {
		WindowingNode n = new WindowingNode();
		if (obj.containsKey("windows")) {
			WindowsNode wn = parseWindows((JSONArray) obj.get("windows"));
			n.setWindows(wn);
			n.addChild(wn);
		}
		
		Node cn = parseQueryNode(obj);
		if (cn == null) {
			throw new IllegalArgumentException("expected windowing child");
		} else {
			n.setChildNode(cn);
			n.addChild(cn);
		}
		return n;
	}
	
	private WindowsNode parseWindows(JSONArray windowItems) {
		WindowsNode n = new WindowsNode();
		
		for (Object o : windowItems) {
			JSONObject cobj = (JSONObject) o;
			WindowNode cn = parseWindow(cobj);
			n.addWindow(cn);
			n.addChild(cn);
		}
		return n;
	}
	
	private WindowNode parseWindow(JSONObject obj) {
		WindowNode n = new WindowNode();
		n.setName((String) obj.get("name"));
		n.getAttributes().put("name", n.getName());
		
		n.setDefinitionPosition(toLong(obj.get("definition_position")));
		n.getAttributes().put("definition_position", n.getDefinitionPosition());
		
		if (obj.containsKey("using_temporary_table")) {
			n.setUsingTemporaryTable((Boolean) obj.get("using_temporary_table"));
		}
		n.getAttributes().put("usingTemporaryTable", obj.get("using_temporary_table"));

		List<String> functions = parseNameList((JSONArray) obj.get("functions"));
		n.setFunctions(functions);
		n.getAttributes().put("functions", functions);
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

		if (obj.containsKey("insert") && newLayout) {
			n.setInsert((Boolean) obj.get("insert"));
			n.getAttributes().put("insert", n.isInsert());
		}
		
		n.getAttributes().put("distinct", obj.get("distinct")); // boolean
		n.getAttributes().put("usingIndex", obj.get("using_index")); // boolean
		
		n.setAccessType(AccessTypeEnum.fromJsonValue((String) obj.get("access_type")));
		n.getAttributes().put("accessType", obj.get("access_type")); // "ALL", "ref", "eq_ref"
		
		List<String> possibleKeys = parseNameList((JSONArray) obj.get("possible_keys"));
		n.setPossibleKeys(possibleKeys);
		n.getAttributes().put("possibleKeys", possibleKeys); // List<String>
		
		if (obj.containsKey("key")) { // "PRIMARY" 
			n.setKey((String) obj.get("key")); 
			n.getAttributes().put("key", n.getKey()); 
		} 
		
		n.setUsedKeyParts(parseNameList((JSONArray) obj.get("used_key_parts")));
		n.getAttributes().put("usedKeyParts", n.getUsedKeyParts()); // List<String>, subset of above ?
		
		n.setUsedColumns(parseNameList((JSONArray) obj.get("used_columns")));
		n.getAttributes().put("usedColumns", n.getUsedColumns());
		
		if (obj.containsKey("key_length")) {
			n.setKeyLength(Long.parseLong((String) obj.get("key_length")));
			n.getAttributes().put("keyLength", n.getKeyLength()); 
		}
		
		if (obj.containsKey("ref")) {
			n.setRefs(parseNameList((JSONArray) obj.get("ref")));
			n.getAttributes().put("ref", n.getRefs()); 
		}
		
		if (obj.containsKey("rows_examined_per_scan")) { 
			n.setRowsExaminedPerScan(((Number) obj.get("rows_examined_per_scan")).longValue());
			n.getAttributes().put("rows_examined_per_scan", n.getRowsExaminedPerScan()); 
			}
		if (obj.containsKey("rows_produced_per_join")) { 
			n.setRowsProducedPerJoin(((Number) obj.get("rows_produced_per_join")).longValue()); 
			n.getAttributes().put("rows_produced_per_join", n.getRowsProducedPerJoin()); 
			}
		if (obj.containsKey("filtered")) {
			n.setFiltered(Double.parseDouble((String) obj.get("filtered")));
			n.getAttributes().put("filtered", n.getFiltered()); 
		}
		n.getAttributes().put("index_condition", obj.get("index_condition"));
		
		if (obj.containsKey("cost_info")) { 
			n.setCostInfo(parseCostInfo((JSONObject) obj.get("cost_info"))); 
			n.getAttributes().put("costInfo", n.getCostInfo()); 
		}

		n.setAttachedCondition((String) obj.get("attached_condition"));
		n.getAttributes().put("attachedCondition", n.getAttachedCondition());
		
		if (obj.containsKey("attached_subqueries")) {
			AttachedSubqueriesNode sn = new AttachedSubqueriesNode();
			n.setAttachedSubqueries(sn);
			n.addChild(sn);
			JSONArray qs = (JSONArray) obj.get("attached_subqueries");
			for (Object o : qs) {
				JSONObject cobj = (JSONObject) o;
				QuerySpecificationNode qsn = parseQuerySpecification(cobj);
				sn.addQuerySpecification(qsn);
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