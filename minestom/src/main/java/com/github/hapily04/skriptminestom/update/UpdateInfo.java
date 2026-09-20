package com.github.hapily04.skriptminestom.update;

import org.jetbrains.annotations.Nullable;

/**
 * Metadata for a GitHub release of skript-minestom.
 */
public record UpdateInfo(String tagName, String releaseName, String body, String htmlUrl, @Nullable String downloadUrl,
						 @Nullable String assetName) { }
