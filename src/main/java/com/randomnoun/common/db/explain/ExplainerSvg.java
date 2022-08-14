package com.randomnoun.common.db.explain;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;

import org.apache.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.randomnoun.common.StreamUtil;
import com.randomnoun.common.db.explain.graph.Box;
import com.randomnoun.common.db.explain.graph.Layout;
import com.randomnoun.common.db.explain.visitor.RangeVisitor;
import com.randomnoun.common.db.explain.visitor.ReweightVisitor;
import com.randomnoun.common.db.explain.visitor.SvgBoxVisitor;
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
	
	public ExplainerSvg() {
		
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
		b.setPosX(b.getPosX() - rv.getMinX());
		b.setPosY(b.getPosY() - rv.getMinY());
		
		ReweightVisitor rwv = new ReweightVisitor(rv.getMinWeight(), rv.getMaxWeight());
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
