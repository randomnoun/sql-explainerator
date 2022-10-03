package com.randomnoun.common.db.explain;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.json.simple.parser.ParseException;

public class SqlExplaineratorWindowTest extends SqlExplaineratorTestBase {

	Logger logger = Logger.getLogger(SqlExplaineratorWindowTest.class);
	
	public void testWindowFunctions() throws IOException, ParseException {
		testParser("window/window-1");
		testSvg("window/window-1");

		testParser("window/window-2");
		testSvg("window/window-2");

		testParser("window/window-3");
		testSvg("window/window-3");

		testParser("window/window-4");
		testSvg("window/window-4");

		testParser("window/window-5");
		testSvg("window/window-5");

	}
	
	
	public void testHavingFunctions() throws IOException, ParseException {
		testParser("window/having-1");
		testSvg("window/having-1");

		testParser("window/having-2");
		testSvg("window/having-2");
	}
	
	public void testInsertFunctions() throws IOException, ParseException {
		testParser("window/insert-1");
		testSvg("window/insert-1");
	}
		
	
}
