package com.sixsq.slipstream.stats;

import com.sixsq.slipstream.persistence.Run;

public class MeasurementsFactory {

	public static Measurements get(Run run) {

		Measurements ms = null;

		switch (run.getType()) {
		case Machine:
			ms = new BuildImageMeasurements();
			break;
		case Orchestration:
			ms = new DeploymentMeasurements();
			break;
		case Run:
			ms = new SimpleRunMeasurements();
			break;
		default:
			ms = null;
		}
		return ms;
	}
}
