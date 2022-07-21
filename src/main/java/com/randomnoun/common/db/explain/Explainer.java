package com.randomnoun.common.db.explain;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.randomnoun.common.StreamUtil;
import com.randomnoun.common.log4j.Log4jCliConfiguration;

/* Based on the explain renderer in MySQL Workbench, although it turns out that that code is GPLed
 * so I'll have to clean-room this instead.
 * 
 * TODO: run a bunch of query plans and see what kinds of images it produces.
 * 
 */

public class Explainer {

	static Logger logger = Logger.getLogger(Explainer.class);
	
	public static class Node {
		
	}
	
	public static class ExplainContext {
		/* #
# JSON Explain Data Parser
#
# Takes a JSON object (bunch of dicts and lists) and turns into internal node objects
# */

		private List<Node> nodes;
		String json;
		
		public ExplainContext(String json, String server_version) throws ParseException {
			this.json = json;
			this.nodes = getNodes(json);
		}
		
		public List<Node> getNodes(String json) throws ParseException {
			JSONParser jp = new JSONParser();
			JSONObject obj = (JSONObject) jp.parse(json);
			List<Node> result = new ArrayList<>();
		
			result.add(parse((JSONObject) obj.get("query_block")));
			return result;
		}

		private Node parse(JSONObject object) {
			return null;
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
		// logger.info(baos.toString());
		
		ExplainContext ec = new ExplainContext(baos.toString(), "1.2.3");
		
		
	}
	
}
