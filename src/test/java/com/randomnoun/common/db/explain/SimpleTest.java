package com.randomnoun.common.db.explain;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;

import org.json.simple.parser.ParseException;

import com.randomnoun.common.db.explain.enums.TooltipTypeEnum;
import com.randomnoun.common.db.explain.layout.ExplaineratorLayout;
import com.randomnoun.common.db.explain.parser.PlanParser;

import junit.framework.TestCase;

/** Simple test */
public class SimpleTest extends TestCase {

	public void testExplainerator() throws IOException, ParseException {
		
		// simple test input
		InputStream is = SqlExplainerator.class.getResourceAsStream("/sakila/sakila-1.json");
		Reader r = new InputStreamReader(is);

		SqlExplainerator seti = new SqlExplainerator();
		seti.setPlanParser(new PlanParser("8.0", true));
		seti.setLayout(new ExplaineratorLayout());
		seti.parseJson(r);
		
		seti.setTooltipType(TooltipTypeEnum.ATTRIBUTE_JS);
		seti.setCss(null);    // default css
		seti.setScript(null); // default javascript
		
		seti.writeSvg(new PrintWriter(System.out));
		seti.writeHtml(new PrintWriter(System.out));
	}

}
