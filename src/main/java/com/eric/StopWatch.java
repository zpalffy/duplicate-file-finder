package com.eric;

public class StopWatch {

	private long start;

	private String duration;

	private StopWatch() {
	}

	public static StopWatch start() {
		return start(System.currentTimeMillis());
	}
	
	public static StopWatch start(long startTime) {
		StopWatch retVal = new StopWatch();
		retVal.start = startTime;
		return retVal;
	}

	public StopWatch restart() {
		duration = null;
		start = System.currentTimeMillis();
		return this;
	}

	public StopWatch stop() {
		long d = System.currentTimeMillis() - start;

		long hours = d / (60L * 60L * 1000L);
		d = d % (60L * 60L * 1000L);
		long minutes = d / (60L * 1000L);
		d = d % (60 * 1000);
		long seconds = d / 1000;
		long ms = d % 1000;

		if (hours > 0) {
			duration = String.format("%d:%02d:%02d.%03d", hours, minutes, seconds, ms);
		} else if (minutes > 0) {
			duration = String.format("%d:%02d.%03d m.", minutes, seconds, ms);
		} else if (seconds > 0) {
			duration = String.format("%d.%03d s.", seconds, ms);
		} else {
			duration = String.format("%d ms.", ms);
		}

		return this;
	}

	@Override
	public String toString() {
		return duration;
	}
}
