package com.randomnoun.common.db.explain;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.json.simple.parser.ParseException;

public class SqlExplaineratprSaqilaTest extends SqlExplaineratorTestBase {

	Logger logger = Logger.getLogger(SqlExplaineratprSaqilaTest.class);
	
	public void testSomeThings() throws IOException, ParseException {
		testParser("sakila/sakila-7g");
		testSvg("sakila/sakila-7g");
	}
	
}
