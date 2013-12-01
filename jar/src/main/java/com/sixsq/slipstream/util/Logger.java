package com.sixsq.slipstream.util;

public class Logger {

	private static java.util.logging.Logger getLogger() {
		return java.util.logging.Logger.getLogger("SlipStreamServer");
	}
	
	public static void info(String message) {
		getLogger().info(message);
	}
	
	public static void warning(String message) {
		getLogger().warning(message);
	}
	
	public static void severe(String message) {
		getLogger().severe(message);
	}
}
