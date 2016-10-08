/*
 * Copyright © 2016 Keith Packard <keithp@keithp.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 */

/*
 * This code uses ideas and algorithms from Dire Wold, an amateur
 * radio packet TNC which was written by John Langner, WB2OSZ. That
 * project is also licensed under the GPL, either version 2 of the
 * License or (at your option) any later version.
 */

package org.altusmetrum.aprslib_1;

import java.util.*;
import java.io.*;

public class AprsDsp {

	static public final int window_cosine = 0;
	static public final int window_hamming = 1;
	static public final int window_blackman = 2;
	static public final int window_flattop = 3;
	static public final int window_truncated = 4;

	static public final float pi = (float) Math.PI;

	static public float cos(float f) {
		return (float) Math.cos((double) f);
	}

	static public float sin(float f) {
		return (float) Math.sin((double) f);
	}

	static public float window (int type, int size, int j) throws IllegalArgumentException {
		float center;
		float w = 1.0f;

		center = 0.5f * (size - 1);

		switch (type) {
		case window_cosine:
			w = cos((float) (j - center) / size * pi);
			break;

		case window_hamming:
			w = 0.53836f - 0.46164f * cos((float) (j * 2 * pi) / (size - 1));
			break;

		case window_blackman:
			w =  0.42659f - 0.49656f * cos((float) (j * 2 * pi) / (size - 1))
				+ 0.076849f * cos((float) (j * 4 * pi) / (size - 1));
			break;

		case window_flattop:
			w =  1.0f - 1.93f  * cos((float)(j * 2 * pi) / (size - 1))
				  + 1.29f  * cos((float)(j * 4 * pi) / (size - 1))
				  - 0.388f * cos((float)(j * 6 * pi) / (size - 1))
				  + 0.028f * cos((float)(j * 8 * pi) / (size - 1));
			break;

		case window_truncated:
			w = 1.0f;
			break;
		default:
			throw new IllegalArgumentException(String.format("Unknown window %d", type));
		}
		return w;
	}
}
