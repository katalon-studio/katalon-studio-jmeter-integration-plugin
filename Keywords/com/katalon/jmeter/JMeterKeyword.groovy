package com.katalon.jmeter;

import com.kms.katalon.core.annotation.Keyword;
import com.kms.katalon.core.configuration.RunConfiguration;

import internal.GlobalVariable;

import org.apache.jmeter.config.Arguments;

class JMeterKeyword {
	@Keyword
	def createSampler(Closure testAction) {
		KatalonSampler sampler = new KatalonSampler(testAction);

		String directoryPath = RunConfiguration.getReportFolder() + "/requests";
		Arguments args = new Arguments();
		args.addArgument("REPORT_DIR", directoryPath);
		sampler.setArguments(args);
		return sampler;
	}
}
