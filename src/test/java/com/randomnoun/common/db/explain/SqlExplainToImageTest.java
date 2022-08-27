package com.randomnoun.common.db.explain;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;

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
	
    private static boolean WRITE_EXPECTED_OUTPUT = true;	
	
	public void setUp() throws IOException {
		Log4jCliConfiguration lcc = new Log4jCliConfiguration();
		lcc.init("[explain-to-image]", null);

		// File f = new File("target/test/sakila");
		// logger.info(f.getCanonicalPath());
		// f.mkdirs();
	}
	
	public void testSomeThings() throws IOException, ParseException {
		
		testParser("sakila/sakila-7g"); // .json
		testSvg("sakila/sakila-7g");
	}
	
	public void testParser(String resourceName) throws IOException, ParseException {
		InputStream is = SqlExplainToImage.class.getResourceAsStream("/" + resourceName + ".json");
		ByteArrayOutputStream baos = new ByteArrayOutputStream(); 
		StreamUtil.copyStream(is, baos);
		String json = baos.toString();
		CompactJsonWriter w = new CompactJsonWriter();
		JSONParser jp = new JSONParser();
		JSONObject obj = (JSONObject) jp.parse(json);
		
		/*
		// ByteArrayOutputStream out1 = new ByteArrayOutputStream();
		FileOutputStream out1 = new FileOutputStream("target/test/" + resourceName + "-compact.json");
		PrintWriter pw = new PrintWriter(out1);
		w.writeJSONValue(obj, pw);
		pw.flush();
		out1.close();
		*/
		
		PlanParser pp = new PlanParser(json, "1.2.3");
		json = pp.toJson();
		obj = (JSONObject) jp.parse(json);
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        w.writeJSONValue(obj, pw);
        pw.flush();
        
        File tf = new File("src/test/resources/expected-output/" + resourceName + "-roundtrip.json");
        if (WRITE_EXPECTED_OUTPUT) {
            FileOutputStream fos = new FileOutputStream(tf);
            fos.write(sw.toString().getBytes());
    		fos.close();
        } else {
            FileInputStream fis = new FileInputStream(tf);
            String expected = new String(StreamUtil.getByteArray(fis));
            fis.close();
            assertEquals("difference in " + resourceName + "-roundtrip.json", expected.trim(), sw.toString().trim());
        }
	}
	
	public void testSvg(String resourceName) throws IOException, ParseException {
		InputStream is = SqlExplainToImage.class.getResourceAsStream("/" + resourceName + ".json");
		ByteArrayOutputStream baos = new ByteArrayOutputStream(); 
		StreamUtil.copyStream(is, baos);
		String json = baos.toString();
		
		PlanParser pp = new PlanParser(json, "1.2.3");
		
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
		
		StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        SvgBoxVisitor sbv = new SvgBoxVisitor(pw, true);
		b.traverse(sbv);
		pw.flush();
		
		File tf = new File("src/test/resources/expected-output/" + resourceName + ".html");
        if (WRITE_EXPECTED_OUTPUT) {
            FileOutputStream fos = new FileOutputStream(tf);
            fos.write(sw.toString().getBytes());
    		fos.close();
        } else {
            FileInputStream fis = new FileInputStream(tf);
            String expected = new String(StreamUtil.getByteArray(fis));
            fis.close();
            assertEquals("difference in " + resourceName + ".html", expected.trim(), sw.toString().trim());
        }

        
		sw = new StringWriter();
        pw = new PrintWriter(sw);
        sbv = new SvgBoxVisitor(pw, false);
		b.traverse(sbv);
		pw.flush();
		
		tf = new File("src/test/resources/expected-output/" + resourceName + ".svg");
        if (WRITE_EXPECTED_OUTPUT) {
            FileOutputStream fos = new FileOutputStream(tf);
            fos.write(sw.toString().getBytes());
    		fos.close();
        } else {
            FileInputStream fis = new FileInputStream(tf);
            String expected = new String(StreamUtil.getByteArray(fis));
            fis.close();
            assertEquals("difference in " + resourceName + ".svg", expected.trim(), sw.toString().trim());
        }

	}
	
}
