package com.tencent.cloud.polaris.config.logger;

/**
 * @author juanyinyang
 */
public enum Level {

	/** log level. */
	TRACE("TRACE"), DEBUG("DEBUG"), INFO("INFO"), WARN("WARN"), ERROR("ERROR"), FATAL("FATAL"), OFF("OFF");

	private String level;

	Level(String level) {
		this.level = level;
	}
	public String getLevel() {
		return level;
	}

	public static Level levelOf(String level) {
		for (Level l : Level.values()) {
			if (l.level.equalsIgnoreCase(level)) {
				return l;
			}
		}
		return null;
	}
}
