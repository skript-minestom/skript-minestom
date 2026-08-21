package com.github.hapily04.skriptminestom.util;

import org.bukkit.Bukkit;

import java.io.*;

public class FileUtils {

	private FileUtils() {}

	/**
	 * Ensures that the provided file and its directories are created.
	 *
	 * @param file the file to defend
	 * @return the file being defended (ignorable)
	 */
	public static File defendFile(File file) {
		return defendFile(file, false);
	}

	/**
	 * Ensures that the provided file and its directories are created.
	 *
	 * @param file the file to defend
	 * @param directory is the file provided intended to be a directory?
	 * @return the file being defended (ignorable)
	 */
	@SuppressWarnings("ResultOfMethodCallIgnored")
	public static File defendFile(File file, boolean directory) {
		try {
			File parent = file.getParentFile();
			if (!parent.exists()) {
				parent.mkdirs();
			}
			if (directory) file.mkdir();
			else file.createNewFile();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return file;
	}

	public static File getServerDirectory() {
		return Bukkit.getServerDirectory();
	}

	public static void setServerDirectory(File serverDirectory) {
		Bukkit.setServerDirectory(serverDirectory);
	}

}
