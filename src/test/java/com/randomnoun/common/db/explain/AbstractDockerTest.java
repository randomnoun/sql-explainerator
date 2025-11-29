package com.randomnoun.common.db.explain;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;
import org.junit.jupiter.api.AfterAll;

import com.google.common.base.Optional;
import com.google.common.net.HostAndPort;
import com.randomnoun.common.Text;
import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerCertificates;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.DockerClient.LogsParam;
import com.spotify.docker.client.DockerHost;
import com.spotify.docker.client.EventStream;
import com.spotify.docker.client.LogStream;
import com.spotify.docker.client.exceptions.DockerCertificateException;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.exceptions.DockerRequestException;
import com.spotify.docker.client.messages.ContainerInfo;
import com.spotify.docker.client.messages.ContainerMount;
import com.spotify.docker.client.messages.Event;
import com.spotify.docker.client.messages.NetworkSettings;
import com.spotify.docker.client.messages.PortBinding;

public abstract class AbstractDockerTest {

	static Logger logger = Logger.getLogger(AbstractDockerTest.class);
	static Logger stdoutLogger = Logger.getLogger("com.randomnoun.common.db.explain.AbstractDockerTest.<stdout>");
	static Logger stderrLogger = Logger.getLogger("com.randomnoun.common.db.explain.AbstractDockerTest.<stderr>");

	// shared between tests; junit 4 puts common test state in static vars. eurgh.
	protected static DockerClient docker;
	protected static String containerId;
	protected static Connection conn;
	
    @AfterAll
    public static void tearDownClass() {
		// remove the container if it's still around
		logger.info("cleanup");
		if (containerId!=null) {
			try {
				logger.info("========= stdout is");
				LogStream dls = docker.logs(containerId, LogsParam.stdout());
				logger.info(dls.readFully());
			} catch (Exception e) {
				logger.error("Could not get stdout", e);
			}
			try {
				logger.info("========= stderr is");
				LogStream dls = docker.logs(containerId, LogsParam.stderr());
				logger.info(dls.readFully());
			} catch (Exception e) {
				logger.error("Could not get stdout", e);
			}
			
			try { 
				docker.killContainer(containerId);
			} catch (Exception e) {
				if (e instanceof DockerRequestException) { logger.error(((DockerRequestException)e).message()); }
				logger.error("Could not kill container", e);
			}
			try { 
				docker.removeContainer(containerId);
			} catch (Exception e) {
				if (e instanceof DockerRequestException) { logger.error(((DockerRequestException)e).message()); }
				logger.error("Could not remove container", e);
			}
		}
		if (docker!=null) {
			docker.close();
		}
    }
    
    @SuppressWarnings("java:S2925") // allow Thread.sleep() in test classes
    static DockerClient getDocker() throws DockerCertificateException {
		TestConfig testConfig = TestConfig.getTestConfig();
		String dockerCertPath = testConfig.getProperty("docker.certPath");
		String dockerHost = testConfig.getProperty("docker.host");
		
		final DockerClient docker = partialBuilder(dockerHost, dockerCertPath).build();
		
		logger.info("========= attaching event stream");
		ExecutorService executor = Executors.newFixedThreadPool(1); // other thread is for stdout/stderr logging
		// see https://www.mankier.com/1/docker-events
		// and http://gliderlabs.com/devlog/2015/docker-events-explained/
		// containers report these: attach, commit, copy, create, destroy, detach, die, exec_create, exec_detach, exec_start, export, kill, oom, pause, rename, resize, restart, start, stop, top, unpause, update
		// images report these: delete, import, load, pull, push, save, tag, untag
		// volumes report these: create, mount, unmount, destroy
		// networks report these: create, connect, disconnect, destroy
		executor.submit(new Callable<Void>() {
			public Void call() throws Exception {
				EventStream es = docker.events();
				while (true) {
					while (es.hasNext()) {
						Event e = es.next();
						logger.info("Event: " + e);
					}
					try { 
						Thread.sleep(100); 
					} catch (InterruptedException ie) { 
						Thread.currentThread().interrupt();
						throw new IllegalStateException("Interrupted thread", ie);
					}
				}
			}
		});
		return docker;
    }
    
    

	// similar sort of thing as DefaultDockerClient.fromEnv(), except it's not from the env
	static DefaultDockerClient.Builder partialBuilder(String uri, String dockerCerts) throws DockerCertificateException {
		/*
		String endpoint = uri;
		Path dockerCertPath = Paths.get(dockerCerts);
		DefaultDockerClient.Builder builder = DefaultDockerClient.builder();
	    final Optional<DockerCertificates> certs = DockerCertificates.builder()
	        .dockerCertPath(dockerCertPath).build();

	      final String stripped = endpoint.replaceAll(".*://", "");
	      final HostAndPort hostAndPort = HostAndPort.fromString(stripped);
	      final String hostText = hostAndPort.getHostText();
	      final String scheme = certs.isPresent() ? "https" : "http";

	      final int port = hostAndPort.getPortOrDefault(DockerHost.defaultPort());
	      final String address = Text.isBlank(hostText) ? DockerHost.defaultAddress() : hostText;

	      builder.uri(scheme + "://" + address + ":" + port);

	    if (certs.isPresent()) {
	      builder.dockerCertificates(certs.get());
	    }
	    */
		
		DefaultDockerClient.Builder builder = DefaultDockerClient.builder().uri("npipe:////./pipe/docker_engine");
	    return builder;
	}

	

	
	protected static void inspectContainer(DockerClient docker, String containerId) throws DockerException, InterruptedException {
		final ContainerInfo info = docker.inspectContainer(containerId);
		logger.info("info=" + info);
		
		NetworkSettings ns = info.networkSettings();
		if (ns.portMapping()!=null) {
			for (Entry<String, Map<String, String>>  pm : ns.portMapping().entrySet()) {
				logger.info("  portMapping " + pm.getKey());
				for (Entry<String, String> pme : pm.getValue().entrySet()) {
				    logger.info("    portMappintEntry " + pme.getKey() + " = " + pme.getValue());
				}
			}
		}
		if (ns.ports()!=null) {
			for (Entry<String, List<PortBinding>>  p : ns.ports().entrySet()) {
				logger.info("  port " + p.getKey());
				for (PortBinding pb : p.getValue()) {
					logger.info("    portBinding hostIp:hostPort " + pb.hostIp() + ":" + pb.hostPort());
				}
			}
		}
		
		final List<ContainerMount> mounts = info.mounts();
		for (ContainerMount cm : mounts) {
			logger.info("  mount mode=" + cm.mode() + ", rw=" + cm.rw() + ", source=" + cm.source() + ", destination=" + cm.destination() );
		}
		 
	}
	
}
