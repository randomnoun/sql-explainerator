package com.randomnoun.common.db.explain;

import java.io.ByteArrayOutputStream;
import java.io.File;
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
import com.randomnoun.common.db.explain.layout.Layout;
import com.randomnoun.common.db.explain.parser.PlanParser;
import com.randomnoun.common.db.explain.visitor.RangeBoxVisitor;
import com.randomnoun.common.db.explain.visitor.ReweightBoxVisitor;
import com.randomnoun.common.db.explain.visitor.SvgBoxVisitor;
import com.randomnoun.common.log4j.Log4jCliConfiguration;

import junit.framework.TestCase;

public class SqlExplainToImageTest extends TestCase {

	Logger logger = Logger.getLogger(SqlExplainToImageTest.class);
	
	public void setUp() throws IOException {
		Log4jCliConfiguration lcc = new Log4jCliConfiguration();
		lcc.init("[explain-to-image]", null);

		File f = new File("target/test");
		logger.info(f.getCanonicalPath());
		f.mkdirs();

	}
	
	public void testSomeThings() throws IOException, ParseException {
		
		testParser("somequery.json");
		testSvg("somequery.json");
	}
	
	public void testParser(String resourceName) throws IOException, ParseException {
		InputStream is = SqlExplainToImage.class.getResourceAsStream("/" + resourceName);
		ByteArrayOutputStream baos = new ByteArrayOutputStream(); 
		StreamUtil.copyStream(is, baos);
		String json = baos.toString();
		
		CompactJsonWriter w = new CompactJsonWriter();
		JSONParser jp = new JSONParser();
		JSONObject obj = (JSONObject) jp.parse(json);
		// ByteArrayOutputStream out1 = new ByteArrayOutputStream();
		FileOutputStream out1 = new FileOutputStream("target/test/out1.json");
		PrintWriter pw = new PrintWriter(out1);
		w.writeJSONValue(obj, pw);
		pw.flush();
		out1.close();
		
		
		PlanParser pp = new PlanParser(json, "1.2.3");
		
		// roundtrip attempt
		json = pp.toJson();
		// System.out.println(json);
		obj = (JSONObject) jp.parse(json);
		FileOutputStream out2 = new FileOutputStream("target/test/out2.json");
		pw = new PrintWriter(out2);
		w.writeJSONValue(obj, pw);
		pw.flush();
		// System.out.println(out1.toString());
		out2.close();
	}
	
	public void testSvg(String resourceName) throws IOException, ParseException {
		InputStream is = SqlExplainToImage.class.getResourceAsStream("/" + resourceName);
		ByteArrayOutputStream baos = new ByteArrayOutputStream(); 
		StreamUtil.copyStream(is, baos);
		String json = baos.toString();
		
		PlanParser pp = new PlanParser(json, "1.2.3");
		
		// roundtrip attempt
		json = pp.toJson();
		
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
		
		FileOutputStream out3 = new FileOutputStream("target/test/out2.html");
		PrintWriter pw = new PrintWriter(out3);
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
