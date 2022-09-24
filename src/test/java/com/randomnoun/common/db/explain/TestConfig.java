package com.randomnoun.common.db.explain;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.Properties;

import com.randomnoun.common.PropertyParser;

/** An object to hold test configuration data.
 * 
 * <p>Similar to the AppConfig, but without the byzantine initialisation logic.
 * 
 * @author knoxg
 */
public class TestConfig extends Properties {

	/** Generated serialVersionUID */
	private static final long serialVersionUID = -2339270651616483239L;
	private static TestConfig instance = null;
	
	public static TestConfig getTestConfig() {
		if (instance==null) {
			try {
				TestConfig newInstance = new TestConfig();
				newInstance.loadProperties();
				instance = newInstance;
			} catch (Exception e) {
				throw new IllegalStateException("Could not initialise TestConfig", e);
			}
		}
		return instance;
	}
	
	private void loadProperties() throws ParseException, IOException {

		// defaults set in classpath resource, overridden by properties on filesystem
		InputStream is = this.getClass().getClassLoader().getResourceAsStream("/sql-explainerator-test.properties");
        if (is==null) {
        	// command-line support
        	is = this.getClass().getClassLoader().getResourceAsStream("sql-explainerator-test.properties");
        }
        if (is==null) {
            System.out.println("Could not find internal property file");
        } else {
            PropertyParser parser = new PropertyParser(new InputStreamReader(is)); // "hostname"
            Properties props = parser.parse();
            this.putAll(props);
            is.close();
        }
		
		// docker properties
		String os = System.getProperty("os.name");
		String pathname = System.getProperty("com.randomnoun.unitTestPath");
		if (pathname==null) {
			pathname = (os.toLowerCase().indexOf("win")>=0) ? "c:\\data\\unit-test-data" : "/opt/unit-test-data";
		}
		
		File path = new File(pathname);
		File testDataFile = new File(path, "sql-explainerator/sql-explainerator-test.properties");
		Properties fsProps;
		try {
			fsProps = new PropertyParser(new FileReader(testDataFile)).parse();
			fsProps.setProperty("unitTest.unitTestPath", pathname.toString());
		} catch (Exception e) {
			throw new RuntimeException("Could not load '" + testDataFile.getAbsolutePath() + "'. " +
		      "The path to this file can be modified by setting the 'com.randomnoun.unitTestPath' system property", e);
		}
		this.putAll(fsProps);
	}
	
}
