/*
 * Copyright © 2016 Keith Packard <keithp@keithp.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
 */

package org.altusmetrum.aprslib_1;

public class AprsGoertzel {
	static private final float pi = (float) Math.PI;

	static final private float sinf(float f) {
		return (float) Math.sin(f);
	}

	static final private float cosf(float f) {
		return (float) Math.cos(f);
	}

	static final private float hamming(int j, int length) {
		return  0.53836f - 0.46164f * cosf((j * 2 * pi) / (length - 1));
	}

	int	length;

	float	coeff;

	float[]	window;

	static public final int window_cosine = 1;
	static public final int window_hamming = 2;
	static public final int window_blackman = 3;
	static public final int window_flattop = 4;
	static public final int window_truncated = 5;

	private float window (int type, int size, int j) {
		float center;
		float w = 1.0f;

		center = 0.5f * (size - 1);

		switch (type) {
		case window_cosine:
			w = cosf((float) (j - center) / size * pi);
			break;

		case window_hamming:
			w = 0.53836f - 0.46164f * cosf((j * 2 * pi) / (size - 1));
			break;

		case window_blackman:
			w =  0.42659f - 0.49656f * cosf((j * 2 * pi) / (size - 1))
				+ 0.076849f * cosf((j * 4 * pi) / (size - 1));
			break;

		case window_flattop:
			w =  1.0f - 1.93f  * cosf((j * 2 * pi) / (size - 1))
				  + 1.29f  * cosf((j * 4 * pi) / (size - 1))
				  - 0.388f * cosf((j * 6 * pi) / (size - 1))
				  + 0.028f * cosf((j * 8 * pi) / (size - 1));
			break;

		case window_truncated:
		default:
			w = 1.0f;
			break;
		}
		return w;
	}

	int window_type = window_hamming;

	public float filter(AprsRing ring) {

		/* Reset */
		float	q2 = 0.0f;
		float	q1 = 0.0f;

		for (int i = 0; i < length; i++) {
			float sample = window[i] * ring.get(i);

			float q0 = coeff * q1 - q2 + sample;
			q2 = q1;
			q1 = q0;
		}
		return q1 * q1 + q2 * q2 - q1 * q2 * coeff;
	}

	public AprsGoertzel (float sample_rate, float frequency, int length) {

		window = new float[length];
		for (int i = 0; i < length; i++)
			window[i] = window(window_type, length, i);

		this.length = length;

		int k = (int) Math.round(length * frequency / sample_rate);

		coeff = 2.0f * cosf(2.0f * pi * k / length);
	}
}
