package com.katalon.jmeter;

import de.sstoehr.harreader.HarReader;
import de.sstoehr.harreader.model.*;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.protocol.java.test.JavaTest;
import org.apache.jmeter.samplers.AbstractSampler;
import org.apache.jmeter.samplers.Entry;
import org.apache.jmeter.samplers.Interruptible;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.testelement.TestStateListener;
import org.apache.jmeter.testelement.property.TestElementProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Callable;

public class KatalonSampler extends AbstractSampler implements TestStateListener, Interruptible {

	private static final Logger log = LoggerFactory.getLogger(JavaTest.class);

	public static final String ARGUMENTS = "arguments";

	/**
	 * The JavaSamplerContext instance used by this sampler to hold information
	 * related to the test run, such as the parameters specified for the sampler
	 * client.
	 */
	private transient JavaSamplerContext context = null;

	private Callable testAction;

	public KatalonSampler() {
		super();
	}

	public KatalonSampler(Callable testAction) {
		super();
		this.testAction = testAction;
	}

	@Override
	public Object clone() {
		KatalonSampler clone = (KatalonSampler) super.clone();
		clone.testAction = this.testAction;
		return clone;
	}

	/**
	 *
	 * @param args
	 *            the new arguments. These replace any existing arguments.
	 */
	public void setArguments(Arguments args) {
		setProperty(new TestElementProperty(ARGUMENTS, args));
	}

	/**
	 *
	 * @return the arguments
	 */
	public Arguments getArguments() {
		return (Arguments) getProperty(ARGUMENTS).getObjectValue();
	}

	@Override
	public boolean interrupt() {
		return false;
	}

	@Override
	public SampleResult sample(Entry e) {
		Arguments args = getArguments();
		args.addArgument(TestElement.NAME, getName()); // Allow Sampler access to test element name

		context = new JavaSamplerContext(args);

		String reportDir = context.getParameter("REPORT_DIR");
		SampleResult result = null;
		try {
			testAction.call();

			final SampleResult[] results = [null];
			Files.find(Paths.get(reportDir), Integer.MAX_VALUE, { path, attributes -> path.toString().endsWith(".har") })
			.each({ path ->
				List<SampleResult> subs = parseHar(path);

				if (!subs.isEmpty()) {
					if (results[0] == null) {
						results[0] = subs.get(0);
						subs.remove(0);
					}

					subs.each({ sub ->
						String name = sub.getSampleLabel();
						results[0].addRawSubResult(sub);
						sub.setSampleLabel(name);
					});
				}
			});
			result = results[0];
		} catch (Exception ex) {
			ex.printStackTrace();
			result = new SampleResult();
			result.setSuccessful(false);
			result.setResponseData(ex.getMessage(), null);
			result.setSampleLabel(ex.getClass().getName());
			log.error("{}\ttestFailed: {}", whoAmI(), ex);
		}

		// Only set the default label if it has not been set
		if (result != null && result.getSampleLabel().length() == 0) {
			result.setSampleLabel(getName());
		}

		return result;
	}

	/**
	 * Generate a String identifier of this instance for debugging purposes.
	 *
	 * @return a String identifier for this sampler instance
	 */
	private String whoAmI() {
		StringBuilder sb = new StringBuilder();
		sb.append(Thread.currentThread().getName());
		sb.append("@");
		sb.append(Integer.toHexString(hashCode()));
		sb.append("-");
		sb.append(getName());
		return sb.toString();
	}

	@Override
	public void testStarted() {
		if (log.isDebugEnabled()) {
			log.debug("{}\ttestStarted", whoAmI());
		}
	}

	@Override
	public void testStarted(String host) {
		if (log.isDebugEnabled()) {
			log.debug("{}\ttestStarted({})", whoAmI(), host);
		}
	}

	@Override
	public void testEnded() {
		if (log.isDebugEnabled()) {
			log.debug("{}\ttestEnded", whoAmI());
		}
	}

	@Override
	public void testEnded(String host) {
		if (log.isDebugEnabled()) {
			log.debug("{}\ttestEnded({})", whoAmI(), host);
		}
	}

	private List<SampleResult> parseHar(Path filePath) {
		List<SampleResult> results = new ArrayList<>();
		try {
			HarReader harReader = new HarReader();
			Har har = harReader.readFromFile(filePath.toFile());
			List<HarEntry> entries = har.getLog().getEntries();

			for (HarEntry entry : entries) {
				HarRequest request = entry.getRequest();
				HarResponse response = entry.getResponse();
				HarContent content = response.getContent();
				HarTiming timing = entry.getTimings();

				SampleResult rs = new SampleResult();
				Integer latency = timing.getWait();
				rs.setLatency(latency);
				rs.setStampAndTime(entry.getStartedDateTime().getTime(), entry.getTime());
				rs.setSampleLabel(request.getUrl());
				rs.setConnectTime(timing.getSend());
				rs.setSuccessful(true);
				rs.setBodySize(response.getBodySize());
				rs.setBytes(content.getSize());
				rs.setContentType(content.getMimeType());
				rs.setDataEncoding(content.getEncoding());
				rs.setIdleTime(timing.getBlocked());
				rs.setResponseCode(String.valueOf(response.getStatus()));
				rs.setResponseMessage(response.getStatusText());
				rs.setURL(new URL(request.getUrl()));
				rs.setSentBytes(request.getBodySize());
				rs.setHeadersSize(request.getHeadersSize().intValue());
				results.add(rs);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return results;
	}
}
