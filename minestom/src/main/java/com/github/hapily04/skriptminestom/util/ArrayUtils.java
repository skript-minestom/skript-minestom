package com.github.hapily04.skriptminestom.util;

public class ArrayUtils {

	public static int[] toIntArray(Object[] boxed) {
		if (boxed == null) return null;
		int[] primitive = new int[boxed.length];
		for (int i = 0; i < boxed.length; i++) primitive[i] = (Integer) boxed[i]; // auto-unboxing
		return primitive;
	}

	public static long[] toLongArray(Object[] boxed) {
		if (boxed == null) return null;
		long[] primitive = new long[boxed.length];
		for (int i = 0; i < boxed.length; i++) primitive[i] = (Long) boxed[i];
		return primitive;
	}

	public static byte[] toByteArray(Object[] boxed) {
		if (boxed == null) return null;
		byte[] primitive = new byte[boxed.length];
		for (int i = 0; i < boxed.length; i++) primitive[i] = (Byte) boxed[i];
		return primitive;
	}

	public static Integer[] toIntegerArray(int[] primitive) {
		if (primitive == null) return null;
		Integer[] boxed = new Integer[primitive.length];
		for (int i = 0; i < primitive.length; i++) {
			boxed[i] = primitive[i]; // auto-boxing
		}
		return boxed;
	}

	public static Long[] toLongArray(long[] primitive) {
		if (primitive == null) return null;
		Long[] boxed = new Long[primitive.length];
		for (int i = 0; i < primitive.length; i++) {
			boxed[i] = primitive[i];
		}
		return boxed;
	}

	public static Byte[] toByteArray(byte[] primitive) {
		if (primitive == null) return null;
		Byte[] boxed = new Byte[primitive.length];
		for (int i = 0; i < primitive.length; i++) {
			boxed[i] = primitive[i];
		}
		return boxed;
	}

}
