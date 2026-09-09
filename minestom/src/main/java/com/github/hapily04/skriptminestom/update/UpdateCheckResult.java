package com.github.hapily04.skriptminestom.update;

import org.jetbrains.annotations.Nullable;

public record UpdateCheckResult(Status status, @Nullable UpdateInfo update) {

	public enum Status {
		UP_TO_DATE,
		UPDATE_AVAILABLE,
		FAILED
	}

	public static UpdateCheckResult upToDate(@Nullable UpdateInfo currentRelease) {
		return new UpdateCheckResult(Status.UP_TO_DATE, currentRelease);
	}

	public static UpdateCheckResult available(UpdateInfo update) {
		return new UpdateCheckResult(Status.UPDATE_AVAILABLE, update);
	}

	public static UpdateCheckResult failed() {
		return new UpdateCheckResult(Status.FAILED, null);
	}

}
