package com.randomnoun.common.db.explain;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.json.simple.parser.ParseException;

public class SqlExplaineratorStackoverflowTest extends SqlExplaineratorTestBase {

	Logger logger = Logger.getLogger(SqlExplaineratorStackoverflowTest.class);
	
	public void testStackoverflowQuestions() throws IOException, ParseException {
		testParser("stackoverflow/q-68136518");
		testSvg("stackoverflow/q-68136518");
	}
	
	
	
}
