package com.github.hapily04.skriptminestom.util;

import net.kyori.adventure.nbt.BinaryTagIO;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.Map;

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

	public static void writeCompound(File file, CompoundBinaryTag compound) {
		file = defendFile(file);
		try (DataOutputStream out = new DataOutputStream(new FileOutputStream(file))) {
			// nameless nbt isn't really written by anybody and is not the standard, so write a blank name like everyone else
			BinaryTagIO.writer().writeNamed(Map.entry("", compound), out, BinaryTagIO.Compression.GZIP);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static @Nullable CompoundBinaryTag readCompound(File file) {
		if (!file.exists()) return null;
		try (DataInputStream in = new DataInputStream(new FileInputStream(file))) {
			return BinaryTagIO.unlimitedReader().readNamed(in, BinaryTagIO.Compression.GZIP).getValue();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static File getServerDirectory() {
		return Bukkit.getServerDirectory();
	}

	public static void setServerDirectory(File serverDirectory) {
		Bukkit.setServerDirectory(serverDirectory);
	}

}
