package com.sixsq.slipstream.metrics;


import java.io.File;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.slf4j.LoggerFactory;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.CsvReporter;
import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Reporter;
import com.codahale.metrics.Slf4jReporter;
import com.codahale.metrics.graphite.Graphite;
import com.codahale.metrics.graphite.GraphiteReporter;
import com.codahale.metrics.graphite.GraphiteReporter.Builder;


public class Metrics {

	private static Metrics instance = null;

	private MetricRegistry registry;
	private List<Reporter> reporters;
	private Map<String, MetricsTimer> timers;


	private Metrics() {
		registry = new MetricRegistry();
		reporters = new ArrayList<Reporter>();
		timers = new ConcurrentHashMap<String, MetricsTimer>();
	}

	private static synchronized Metrics getInstance() {
		if (instance == null) {
			instance = new Metrics();
		}
		return instance;
	}

	@SuppressWarnings("unused")
	private static String generateName(Object instance, String... names) {
		return MetricRegistry.name(instance.getClass(), names);
	}

	private static String generateName(Class<?> klass, String... names) {
		return MetricRegistry.name(klass, names);
	}

	public static MetricsTimer getTimer(Object instance, String... names){
		return getTimer(instance.getClass(), names);
	}

	public static synchronized MetricsTimer getTimer(Class<?> klass, String... names) {
		String name = generateName(klass, names);
		Metrics instance = getInstance();
		if (instance.timers.containsKey(name)) {
			return instance.timers.get(name);
		} else {
			MetricsTimer timer = new MetricsTimer(instance.getRegistry().timer(name));
			instance.timers.put(name, timer);
			return timer;
		}
	}

	public static MetricsTimer newTimer(Object instance, String... names){
		return newTimer(instance.getClass(), names);
	}

	public static MetricsTimer newTimer(Class<?> klass, String... names){
		return new MetricsTimer(getInstance().getRegistry().timer(generateName(klass, names)));
	}

/*
	public void deleteAllReporters() {
		for (Reporter reporter: reporters) {
			reporter.stop();
		}
	}
*/
	public static void addJmxReporter() {
		getInstance().createJmxReporter();
	}

	public static void addConsoleReporter() {
		getInstance().createConsoleReporter();
	}

	public static void addCsvReporter(String filename) {
		getInstance().createCsvReporter(filename);
	}

	public static void addSlf4jReporter() {
		getInstance().createSlf4jReporter();
	}

	public static void addGraphiteReporter() {
		addGraphiteReporter("127.0.0.1");
	}

	public static void addGraphiteReporter(String host) {
		addGraphiteReporter(host, null);
	}

	public static void addGraphiteReporter(String host, int port){
		addGraphiteReporter(host, port, null);
	}

	public static void addGraphiteReporter(String host, String prefix){
		addGraphiteReporter(host, 2003, prefix);
	}

	public static void addGraphiteReporter(String host, int port, String prefix){
		getInstance().createGraphiteReporter(host, port, prefix);
	}

	private MetricRegistry getRegistry(){
		return this.registry;
	}

	private void createJmxReporter() {
		JmxReporter reporter = JmxReporter.forRegistry(registry).build();
		reporters.add(reporter);
		reporter.start();
	}

	private void createConsoleReporter() {
		ConsoleReporter reporter = ConsoleReporter.forRegistry(registry)
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .build();
		reporter.start(1, TimeUnit.MINUTES);
		reporters.add(reporter);
	}

	private void createCsvReporter(String filename) {
		CsvReporter reporter = CsvReporter.forRegistry(registry)
				.formatFor(Locale.US)
				.convertRatesTo(TimeUnit.SECONDS)
				.convertDurationsTo(TimeUnit.MILLISECONDS)
				.build(new File(filename));
		reporter.start(1, TimeUnit.SECONDS);
		reporters.add(reporter);
	}

	private void createSlf4jReporter() {
		Slf4jReporter reporter = Slf4jReporter.forRegistry(registry)
				.outputTo(LoggerFactory.getLogger(this.getClass()))
				.convertRatesTo(TimeUnit.SECONDS)
				.convertDurationsTo(TimeUnit.MILLISECONDS)
				.build();
		reporter.start(1, TimeUnit.MINUTES);
		reporters.add(reporter);
	}

	private void createGraphiteReporter(String host, int port, String prefix) {
		Graphite graphite = new Graphite(new InetSocketAddress(host, port));
		Builder builder = GraphiteReporter.forRegistry(registry)
				.convertRatesTo(TimeUnit.SECONDS)
				.convertDurationsTo(TimeUnit.MILLISECONDS)
				.filter(MetricFilter.ALL);
		if (prefix != null) {
			builder = builder.prefixedWith(prefix);
		}
		GraphiteReporter reporter = builder.build(graphite);
		reporter.start(1, TimeUnit.SECONDS);
		reporters.add(reporter);
	}

}
