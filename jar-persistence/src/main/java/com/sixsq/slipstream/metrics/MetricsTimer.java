package com.sixsq.slipstream.metrics;

import com.codahale.metrics.Timer;

public class MetricsTimer {

	private Timer timer;
	private ThreadLocal<Timer.Context> context = null;

	MetricsTimer(Timer timer) {
		this.timer = timer;
		context = new ThreadLocal<Timer.Context>();
	}

	public MetricsTimer start() {
		if (context.get() != null) {
			throw new IllegalStateException("Cannot start the timer because it has already been started.");
		}
		context.set(timer.time());
		return this;
	}

	public void stop() {
		if (context.get() == null) {
			throw new IllegalStateException("Cannot stop the timer because it hasn't been started.");
		}
		context.get().stop();
		context.set(null);
	}

}
