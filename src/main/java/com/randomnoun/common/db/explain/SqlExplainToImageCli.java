package com.randomnoun.common.db.explain;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.randomnoun.common.StreamUtil;
import com.randomnoun.common.db.explain.graph.Box;
import com.randomnoun.common.db.explain.layout.Layout;
import com.randomnoun.common.db.explain.parser.PlanParser;
import com.randomnoun.common.db.explain.visitor.RangeBoxVisitor;
import com.randomnoun.common.db.explain.visitor.ReweightBoxVisitor;
import com.randomnoun.common.db.explain.visitor.SvgBoxVisitor;
import com.randomnoun.common.log4j.Log4jCliConfiguration;

/** All the CLI options */
public class SqlExplainToImageCli {

	
	
	public static void main(String args[]) throws IOException, ParseException {
		Log4jCliConfiguration lcc = new Log4jCliConfiguration();
		lcc.init("[explain-to-image]", null);
		
		// InputStream is = ExplainerSvg.class.getResourceAsStream("/sakila-7g.json");
		InputStream is = SqlExplainToImage.class.getResourceAsStream("/somequery.json");
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
		
		
		PlanParser pp = new PlanParser();
		pp.parse(json, "1.2.3");
		
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
		Layout layout = new Layout(pp.getTopNode());
		Box b = layout.getLayoutBox();
		
		
		// @TODO translate diagram so that top-left is 0, 0
		RangeBoxVisitor rv = new RangeBoxVisitor();
		b.traverse(rv);
		// logger.info("range [" + rv.getMinX() + ", " + rv.getMinY() + "] - [" + rv.getMaxX() + ", " + rv.getMaxY() + "]");
		b.setPosX(b.getPosX() - rv.getMinX());
		b.setPosY(b.getPosY() - rv.getMinY());
		
		ReweightBoxVisitor rwv = new ReweightBoxVisitor(rv.getMinWeight(), rv.getMaxWeight());
		b.traverse(rwv);

		
		FileOutputStream out3 = new FileOutputStream("c:\\temp\\out2.html");
		pw = new PrintWriter(out3);
		SvgBoxVisitor sbv = new SvgBoxVisitor(pw, true);
		b.traverse(sbv);
		pw.flush();
		out3.close();
		
		
		PrintWriter sysPw = new PrintWriter(System.out);
		sbv = new SvgBoxVisitor(sysPw, true);
		b.traverse(sbv);
		sysPw.flush();
	
		
	}
	
	
}
