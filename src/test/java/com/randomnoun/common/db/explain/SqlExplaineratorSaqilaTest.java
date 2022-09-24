package com.randomnoun.common.db.explain;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.json.simple.parser.ParseException;

public class SqlExplaineratorSaqilaTest extends SqlExplaineratorTestBase {

	Logger logger = Logger.getLogger(SqlExplaineratorSaqilaTest.class);
	
	public void testSomeThings() throws IOException, ParseException {
		testParser("sakila/sakila-1");
		testSvg("sakila/sakila-1");

		testParser("sakila/sakila-4a");
		testSvg("sakila/sakila-4a");

		testParser("sakila/sakila-6a");
		testSvg("sakila/sakila-6a");

		testParser("sakila/sakila-6b");
		testSvg("sakila/sakila-6b");
		
		testParser("sakila/sakila-6c");
		testSvg("sakila/sakila-6c");
		
		testParser("sakila/sakila-7g");
		testSvg("sakila/sakila-7g");
	}
	
}
