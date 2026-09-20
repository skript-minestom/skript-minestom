package com.github.hapily04.skriptminestom.update;

import org.jetbrains.annotations.Nullable;

/**
 * Persisted across an auto-update restart so the new process can delete the old jar
 * and print release notes after scripts finish loading.
 */
public class UpdateState {

	public @Nullable String oldJar;
	public @Nullable String newVersion;
	public @Nullable String releaseName;
	public @Nullable String releaseNotes;
	public @Nullable String htmlUrl;
	/** When true, console should print release notes after Finished loading. */
	public boolean printNotesOnLoad;

	public UpdateState() {}

	public UpdateState(@Nullable String oldJar, @Nullable String newVersion, @Nullable String releaseName, @Nullable String releaseNotes,
					   @Nullable String htmlUrl, boolean printNotesOnLoad) {
		this.oldJar = oldJar;
		this.newVersion = newVersion;
		this.releaseName = releaseName;
		this.releaseNotes = releaseNotes;
		this.htmlUrl = htmlUrl;
		this.printNotesOnLoad = printNotesOnLoad;
	}

}
