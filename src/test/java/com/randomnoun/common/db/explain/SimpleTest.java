package com.randomnoun.common.db.explain;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;

import org.json.simple.parser.ParseException;
import org.junit.jupiter.api.Test;

import com.randomnoun.common.db.explain.enums.TooltipTypeEnum;
import com.randomnoun.common.db.explain.layout.ExplaineratorLayout;
import com.randomnoun.common.db.explain.parser.PlanParser;

/** Simple test */
public class SimpleTest {

	@Test
	public void testExplainerator() throws IOException, ParseException {
		
		// simple test input
		InputStream is = SqlExplainerator.class.getResourceAsStream("/sakila/sakila-1.json");
		Reader r = new InputStreamReader(is);

		SqlExplainerator se = new SqlExplainerator();
		se.setPlanParser(new PlanParser("8.0", true));
		se.setLayout(new ExplaineratorLayout());
		se.parseJson(r);
		
		se.setTooltipType(TooltipTypeEnum.ATTRIBUTE_JS);
		se.setCss(null);    // default css
		se.setScript(null); // default javascript
		
		se.writeSvg(new PrintWriter(System.out));
		se.writeHtml(new PrintWriter(System.out));
	}

}
