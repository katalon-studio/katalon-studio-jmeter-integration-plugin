# Katalon Studio JMeter integration plugin
This plugin provides a custom keyword method `createSampler` that creates a JMeter sampler which will execute JMeter test and parse the Har files to generate JMeter report.

## Manually package the plugin
1. Run command `gradle clean katalonCopyDependencies`
2. Open project with Katalon Studio
3. Run command `gradle clean katalonPluginPackage`
4. The plugin will be available inside `build/libs` directory

## Usage
See the [JMeter integration plugin sample](https://github.com/katalon-studio-samples/jmeter-integration-plugin-sample) for more detail.
