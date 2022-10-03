package com.randomnoun.common.db.explain.json;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.randomnoun.common.Struct;
import com.randomnoun.common.Text;

/** 
 * 	a Node represents an object or array in the mysql-generated query plan json
 *	we subclass this object to more specific node types
 *	 
 *	a Node has two kinds of sub-nodes:
 *	attributes - a loosely-type collection of primitive datatypes and Nodes, used to roundtrip the json
 *	children - contains Nodes only, used to traverse the Node tree. 
 *	   Note that a concrete subclass also contain node-specific fields which may also be contained in children
 * 
 * @author knoxg
 *
 */
public abstract class Node implements Struct.ToJson {
	private String jsonType;
	private boolean isArray;
	private Map<String, Object> attributes = new HashMap<>();
	private List<Node> children = new ArrayList<>();
	
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
					((v instanceof CostInfoNode) ? ((CostInfoNode) v).toJson() :
					((v instanceof NameList) ? ((NameList) v).toJson() :
						
					" UNEXPECTED CLASS " + v.getClass().getName() ))))))); // this is intentionally invalid JSON
				f = false;
			}
			List<Object> remaining = new ArrayList<>();
			for (Object v : children) {
				if (v instanceof Node) {
					Node n = (Node) v;
					s += (f == true ? "": ", ") + "\"" + Text.escapeJavascript(n.jsonType) + "\": " + n.toJson(); 
				} else {
					remaining.add(v);
				}
			}
			//if (remaining.size() > 0) {
			//	throw new IllegalStateException("didn't expect remaining children here");
			//}
			s += "}";
		}
		return s;
	}
	
	protected Stream<Node> reverseStream(List<Node> children) {
		// this is the sort of thing you expect to find at https://stackoverflow.com/questions/24010109/java-8-stream-reverse-order
		// but isn't there
		return IntStream.range(0, children.size())
			.mapToObj(i -> children.get(children.size() - i - 1)); 
	}
	public Map<String, Object> getAttributes() {
		return attributes;
	}
	protected void setAttributes(Map<String, Object> attributes) {
		this.attributes = attributes;
	}
	protected List<Node> getChildren() {
		return children;
	}
	protected void setChildren(List<Node> children) {
		this.children = children;
	}

}