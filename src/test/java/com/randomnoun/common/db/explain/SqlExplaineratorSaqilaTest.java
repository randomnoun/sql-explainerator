package com.randomnoun.common.db.explain;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.json.simple.parser.ParseException;

public class SqlExplaineratorSaqilaTest extends SqlExplaineratorTestBase {

	Logger logger = Logger.getLogger(SqlExplaineratorSaqilaTest.class);
	
	public void testSakilaExamQuestions() throws IOException, ParseException {
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
	
	public void testSakila2() throws IOException, ParseException {
		testParser("sakila2/sakila-2-1");
		testSvg("sakila2/sakila-2-1");
	}
	
	
}
