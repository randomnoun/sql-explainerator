package com.randomnoun.common.db.explain;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.script.ScriptException;
import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import com.randomnoun.common.StreamUtil;
import com.randomnoun.common.Text;
import com.randomnoun.common.db.SqlParser;
import com.randomnoun.common.log4j.Log4jCliConfiguration;
import com.randomnoun.common.log4j.LoggingOutputStream;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.DockerClient.AttachParameter;
import com.spotify.docker.client.exceptions.DockerCertificateException;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.exceptions.ImageNotFoundException;
import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.ContainerCreation;
import com.spotify.docker.client.messages.HostConfig;
import com.spotify.docker.client.messages.PortBinding;

/** Fires up a mysql container and tests the SqlExplainerator against it
 *
 * <p>This class creates the sakila database, and runs the command line against it
 * 
 * @author knoxg
 */
@Ignore 
public class SqlExplaineratorDockerTest extends AbstractDockerTest {

	static Logger logger = Logger.getLogger(SqlExplaineratorDockerTest.class);
	
	public final static int MAX_RETRY_COUNT = 200;
	
	// per-class setup/teardown
	@BeforeClass
    public static void setUpClass() throws DockerException, InterruptedException, DockerCertificateException, ClassNotFoundException, SQLException, UnknownHostException, ParseException {

		String hostname = InetAddress.getLocalHost().getHostName();
		System.out.println("Running on " + hostname);
		if (hostname == null ||  !(hostname.equals("excimer"))) { return; } // for now

		Log4jCliConfiguration lcc = new Log4jCliConfiguration();
		Properties props = new Properties();
		props.put("log4j.revisionedThrowableRenderer.highlightPrefix", "com.randomnoun.,com.somethingelse.*");
		props.put("log4j.logger.org.apache.http", "INFO"); // remove wire logging
		
		lcc.init("[SqlExplaineratorDockerTest]", props);
		try {
			logger.info("Initialising SqlExplaineratorDockerTest");
			docker = getDocker();
			containerId = getMysqlContainerId(docker);
			conn = getMysqlConnection(docker, containerId);
		} catch (Exception e) {
			logger.error("Excepting initialising test", e);
			throw e;
		}
    }

    @AfterClass
    public static void tearDownClass() {
    	AbstractDockerTest.tearDownClass();
    }
    
    
    public static String getMysqlContainerId(final DockerClient docker) throws DockerException, InterruptedException {
		String containerId = null;
				
		// Bind container ports to host ports
		final Map<String, List<PortBinding>> portBindings = new HashMap<>();
		final String[] ports = { "3306" };
		for (String port : ports) {
		    List<PortBinding> hostPorts = new ArrayList<>();
		    hostPorts.add(PortBinding.of("0.0.0.0", 13306));
		    portBindings.put(port, hostPorts);
		}
		
		final HostConfig hostConfig = HostConfig.builder()
			.portBindings(portBindings)
			.build();

		// Create container with exposed ports
		final ContainerConfig containerConfig = ContainerConfig.builder()
			.image("mysql:8.0.30")
			.hostConfig(hostConfig)
		    .env(
		      // "MYSQL_HOST=localhost", // see https://stackoverflow.com/questions/39664141/docker-mysql-container-root-password-mysqld-listens-port-0
		      "MYSQL_ROOT_PASSWORD=abc123", 
		      "MYSQL_DATABASE=sakila",
		      "MYSQL_USER=testuser",  // this user doesn't have SUPER permissions ( required for triggers )
		      "MYSQL_PASSWORD=testpass")
		    .exposedPorts(ports)
		    .cmd("--default-authentication-plugin=mysql_native_password")
		    .build();

		logger.info("========= createContainer");
		ContainerCreation creation;
		try {
			creation = docker.createContainer(containerConfig);
		} catch (ImageNotFoundException infe) {
			logger.info("Image not found; pulling");
			docker.pull("mysql:8.0.30");
			creation = docker.createContainer(containerConfig);
		}
		containerId = creation.id();
		logger.info("creationId = " + containerId);

		// Inspect container
		logger.info("========= inspectContainer (before started)");
		inspectContainer(docker, containerId); 

		// Start container
		logger.info("========= startContainer");
		docker.startContainer(containerId);
		
		logger.info("========= attaching loggers");
		// possible race condition here if there's output before the streams are attached. or is there. 
		final String finalContainerId = containerId;
		ExecutorService executor = Executors.newFixedThreadPool(1); // other thread is for stdout/stderr logging
		executor.submit(new Callable<Void>() {
			public Void call() throws Exception {
				// this blocks if called directly
				docker.attachContainer(finalContainerId,
				   AttachParameter.LOGS, AttachParameter.STDOUT,
				   AttachParameter.STDERR, AttachParameter.STREAM)
				   .attach(new LoggingOutputStream(stdoutLogger), 
						   new LoggingOutputStream(stderrLogger));
				// this happens just after the container is killed.
				logger.info("docker.attacherContainer() complete");
				return null;
			}
		});
		
		logger.info("========= inspectContainer (after started)");
		logger.info("container is " + containerId);
		inspectContainer(docker, containerId);
		
		return containerId;
    }
    
    @SuppressWarnings("java:S2925") // allow Thread.sleep() in test classes
    public static Connection getMysqlConnection(DockerClient docker, String containerId) throws SQLException {
		String url = "jdbc:mysql://" + docker.getHost() + ":13306/sakila";
		logger.info("Connecting to " + url);
		
		int retryCount = 0;
		Connection conn = null;
		while (conn == null) {
			retryCount++;
			// gets up to about 8, or doesn't connect at all
			logger.info("Connecting (attempt " + retryCount + ")..."); 
			try {
				// need to connect as 'root' rather than 'testuser' to get SUPER privileges to create triggers
				conn = DriverManager.getConnection(url, "root", "abc123"); // "testuser", "testpass"
			} catch (SQLException e) {
				logger.error("Connection failure: " + e.getMessage());
				if (retryCount == MAX_RETRY_COUNT) {
					throw e;
				}
				try { 
					Thread.sleep(1000); 
				} catch (InterruptedException ie) { 
					Thread.currentThread().interrupt();
					throw new IllegalStateException("Interrupted thread", ie);
				}

			}
		}
		return conn;
    }


	@Test
	public void testSqlExplaineratorCli() throws DockerCertificateException, DockerException, InterruptedException, IOException, SQLException, ScriptException, ParseException {
		String hostname = InetAddress.getLocalHost().getHostName();
		System.out.println("Running on " + hostname);
		if (hostname == null || !(hostname.equals("excimer"))) { return; } // for now
		
		DataSource ds = new SingleConnectionDataSource(conn, true);
		JdbcTemplate jt = new JdbcTemplate(ds);
		logger.info("databases are " + jt.queryForList("SHOW DATABASES"));

		String sqlFilename = "/setup/sakila-schema.sql";
		try (InputStream is = this.getClass().getResourceAsStream(sqlFilename);
			Reader r = new InputStreamReader(is)) {
			SqlParser sp = new SqlParser();
			sp.consumeStatements(r, false, stmt -> {
				if (!Text.isBlank(stmt)) {
					logger.info("SQL " + stmt);
					jt.execute(stmt);
				}
			});
		} 
		
		sqlFilename = "/setup/sakila-data.sql";
		try (InputStream is = this.getClass().getResourceAsStream(sqlFilename);
			Reader r = new InputStreamReader(is)) {
			SqlParser sp = new SqlParser();
			sp.consumeStatements(r, false, stmt -> {
				if (!Text.isBlank(stmt)) {
					logger.info("SQL " + stmt);
					jt.execute(stmt);
				}
			});
		}

		String url = "jdbc:mysql://" + docker.getHost() + ":13306/sakila";
		
		sqlFilename = "/sakila/sakila-7g.sql";
		try (InputStream is = this.getClass().getResourceAsStream(sqlFilename);
			Reader r = new InputStreamReader(is)) {
			SqlParser sp = new SqlParser();
			sp.consumeStatements(r, false, stmt -> {
				if (!Text.isBlank(stmt)) {
					logger.info("SQL " + stmt);
					PrintStream outStream = System.out;
					try {
						ByteArrayOutputStream baos = new ByteArrayOutputStream();
			            // Redirecting console output to baos
			            System.setOut(new PrintStream(baos));
						SqlExplaineratorCli.main(new String[] {
							"--jdbc", url,
							"--username", "root",
							"--password", "abc123",
							"--sql", stmt
						});
						logger.info("baos is " + baos.toString());
						
				        File tf = new File("src/test/resources/expected-output/sakila/sakila-7g-title.svg");
			            FileInputStream fis = new FileInputStream(tf);
			            String expected = new String(StreamUtil.getByteArray(fis));
			            fis.close();
			            
			            // cost values are slightly different
			            expected = expected.replaceAll("[0-9]+(\\.[0-9]+)?", "999").trim();
			            String actual = baos.toString().replaceAll("[0-9]+(\\.[0-9]+)?", "999").trim();
			            
			            assertEquals("difference in sakila-7g-roundtrip-1.json", expected, actual);
						
					} catch (Exception e) {
						throw new RuntimeException("Exception in main", e);
					} finally {
						System.setOut(outStream);
					}
				}
			});
		}

	}
	
	
	
}
