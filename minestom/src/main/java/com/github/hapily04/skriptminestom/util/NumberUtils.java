package com.github.hapily04.skriptminestom.util;

import ch.njol.skript.util.Timespan;
import net.minestom.server.ServerFlag;
import net.minestom.server.coordinate.Vec;

import java.time.temporal.ChronoUnit;
import java.util.regex.Pattern;

public class NumberUtils {

	private static final Pattern INTEGER_PATTERN = Pattern.compile("\\d+");

	public static boolean isInteger(String input) {
		return INTEGER_PATTERN.matcher(input).matches();
	}

	public static boolean isOnlyDigits(String input) {
		for (char c : input.toCharArray()) {
			if (!Character.isDigit(c)) return false;
		}
		return true;
	}

	public static Timespan timespanFrom(int ticks) {
		return timespanFrom((long) ticks);
	}

	public static Timespan timespanFrom(long ticks) {
		long millis = (1000L * ticks) / ServerFlag.SERVER_TICKS_PER_SECOND;
		return new Timespan(millis);
	}

	public static long ticksFrom(Timespan timespan) {
		return Math.min(Integer.MAX_VALUE, timespan.get(ChronoUnit.MILLIS)/(1000/ServerFlag.SERVER_TICKS_PER_SECOND));
	}

	public static float[] quatFromVec(Vec vec) {
		double x = vec.x();
		double y = vec.y();
		double z = vec.z();
		float c1 = (float) Math.cos(x * 0.5f);
		float c2 = (float) Math.cos(y * 0.5f);
		float c3 = (float) Math.cos(z * 0.5f);

		float s1 = (float) Math.sin(x * 0.5f);
		float s2 = (float) Math.sin(y * 0.5f);
		float s3 = (float) Math.sin(z * 0.5f);

		float qx = s1 * c2 * c3 + c1 * s2 * s3;
		float qy = c1 * s2 * c3 - s1 * c2 * s3;
		float qz = c1 * c2 * s3 - s1 * s2 * c3;
		float qw = c1 * c2 * c3 + s1 * s2 * s3;

		return new float[]{qx, qy, qz, qw};
	}

	// chatgpt
	public static Vec vecFromQuat(float[] quat) {
		float qx = quat[0];
		float qy = quat[1];
		float qz = quat[2];
		float qw = quat[3];
		// Normalize
		float invLen = 1.0f / (float)Math.sqrt(qx*qx + qy*qy + qz*qz + qw*qw);
		qx *= invLen; qy *= invLen; qz *= invLen; qw *= invLen;

		// X
		float sinr_cosp = 2.0f * (qw*qx + qy*qz);
		float cosr_cosp = 1.0f - 2.0f * (qx*qx + qy*qy);
		float x = (float)Math.atan2(sinr_cosp, cosr_cosp);

		// Y
		float sinp = 2.0f * (qw*qy - qz*qx);
		float y;
		if (Math.abs(sinp) >= 1.0f) y = (float)Math.copySign(Math.PI / 2.0, sinp);
		else y = (float)Math.asin(sinp);

		// Z
		float siny_cosp = 2.0f * (qw*qz + qx*qy);
		float cosy_cosp = 1.0f - 2.0f * (qy*qy + qz*qz);
		float z = (float)Math.atan2(siny_cosp, cosy_cosp);

		return new Vec(x, y, z); // radians
	}

}
