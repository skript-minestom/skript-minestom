package com.github.hapily04.skriptminestom.util;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.pattern.color.ForegroundCompositeConverterBase;

public class CustomHighlighter extends ForegroundCompositeConverterBase<ILoggingEvent> {

	@Override
	protected String getForegroundColorCode(ILoggingEvent event) {
		return switch (event.getLevel().toInt()) {
			case Level.ERROR_INT -> "38;5;196";
			case Level.WARN_INT -> "38;5;214";
			default -> "38;5;247";
		};
	}

}