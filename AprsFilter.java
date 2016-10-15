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
 * This code uses ideas and algorithms from Dire Wolf, an amateur
 * radio packet TNC which was written by John Langner, WB2OSZ. That
 * project is also licensed under the GPL, either version 2 of the
 * License or (at your option) any later version.
 */

package org.altusmetrum.aprslib_1;

public class AprsFilter {

	float[]	coeff;

	/* center of the filter (in samples) */
	private float center;

	/* cycles per sample, which is frequency / samples per second */
	private float cps;
	private float cps_low;
	private float cps_high;

	static public final int	filter_cos = 1;
	static public final int	filter_sin = 2;
	static public final int filter_lowpass = 3;
	static public final int filter_bandpass = 4;
	static public final int filter_highpass = 5;

	static public final int window_cosine = 1;
	static public final int window_hamming = 2;
	static public final int window_blackman = 3;
	static public final int window_flattop = 4;
	static public final int window_kaiser = 5;
	static public final int window_truncated = 6;

	static private final float pi = (float) Math.PI;

	static final private float sinf(float f) {
		return (float) Math.sin(f);
	}

	static final private float cosf(float f) {
		return (float) Math.cos(f);
	}

	static final private float sinc(float f) {
		if (f == 0.0f)
			return 1.0f;
		float x = pi * f;
		return sinf(x) / x;
	}

	/* Non-circular convolution, pad with zero
	 *
	 * offset: the position in 'other' which lines up
	 * with coeff[0]. Can be negative, or larger than other
	 */
	private float convolve(float[] other, int offset) {
		int	other_len = other.length;
		int	start, end;

		if (offset < 0) {
			start = -offset;
			end = start + other_len;
		} else {
			start = 0;
			end = other_len - offset;
		}
		if (end > coeff.length)
			end = coeff.length;

		float	sum = 0.0f;
		for (int i = start; i < end; i++)
			sum += other[offset+i] * coeff[i];
		return sum;
	}

	public float convolve(AprsRing ring) {
		float	sum = 0.0f;

		float[] data = ring.data;
		int pos = ring.pos;
		int part = coeff.length - pos;
		int i;
		for (i = 0; i < part; i++)
			sum += data[pos+i] * coeff[i];
		pos -= coeff.length;
		for (; i < coeff.length; i++)
			sum += data[pos+i] * coeff[i];
		return sum;
	}

	public static double bessi0(double x) {
		double ax,ans;
		double y;

		if ((ax=Math.abs(x)) < 3.75) {
			y=x/3.75;
			y*=y;
			ans=1.0+y*(3.5156229+y*(3.0899424+y*(1.2067492
				+y*(0.2659732+y*(0.360768e-1+y*0.45813e-2)))));
		}
		else {
			y=3.75/ax;
			ans=(Math.exp(ax)/Math.sqrt(ax))*(0.39894228+y*(0.1328592e-1
				+y*(0.225319e-2+y*(-0.157565e-2+y*(0.916281e-2
				+y*(-0.2057706e-1+y*(0.2635537e-1+y*(-0.1647633e-1
				+y*0.392377e-2))))))));
		}
		return ans;
	}

	public int length() {
		return coeff.length;
	}

	private float coeff(int filter, int i) throws IllegalArgumentException {
		float	offset = i - center;

		float v, check;
		switch (filter) {
		case filter_cos:
			return cosf(offset * cps * 2 * pi);
		case filter_sin:
			return sinf(offset * cps * 2 * pi);
		case filter_lowpass:
			if (offset == 0.0f)
				v = 2*cps;
			else
				v = sinf(2.0f * cps * pi * offset) / (pi * offset);
			check = 2.0f * cps * sinc(2.0f * cps * offset);
			return v;
		case filter_highpass:
			if (offset == 0)
				return 1.0f;
			return 2.0f * cps * (1 - sinc(2.0f * cps * offset));
		case filter_bandpass:
			if (offset == 0.0f)
				v = 2.0f * (cps_high - cps_low);
			else
				v = sinf(2 * pi * cps_high * offset) / (pi * offset) -
					sinf(2 * pi * cps_low * offset) / (pi * offset);
			check = 2.0f * cps_high * sinc(2.0f * cps_high * offset) -
				2.0f * cps_low * sinc(2.0f * cps_low * offset);
			if (Math.abs(v - check) > 1e-4)
				System.out.printf("bad bandpass old %f new %f\n", v, check);
			return v;
		default:
			throw new IllegalArgumentException(String.format("Unknown filter %d", filter));
		}
	}

	static private float kaiser(int size, int j, float samples_per_second) {
		/* XXX need to have these passed in */
		float ripple = 0.0005f;
		float transition_width = 20.0f;
		float a = -20.0f * (float) Math.log(ripple);
		float tw = 2.0f * pi * transition_width / samples_per_second;
		float m;

		if (a <= 21.0f)
			m = (float) Math.ceil(5.79 / tw);
		else
			m = (float) Math.ceil((a - 7.95f) / (2.285f * tw));

		float beta;

		if (a <= 21.0f)
			beta = 0.0f;
		else if (a <= 50.0f)
			beta = 0.5842f * (float) Math.pow(a - 21.0f, 0.4f) - 0.07886f * (a - 21.0f);
		else
			beta = 0.1102f * (a - 8.7f);

		float w = (float) (bessi0(beta * Math.sqrt(1 - ((2 * size / m - 1) * (2 * size / m - 1)))) / bessi0(beta));
		return w;
	}

	static float window (int type, int size, int j, float samples_per_second) throws IllegalArgumentException {
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
			w = 1.0f;
			break;

		case window_kaiser:
			w = kaiser(size, j, samples_per_second);
			break;
		default:
			throw new IllegalArgumentException(String.format("Unknown window %d", type));
		}
		return w;
	}

	private void init(int size) {
		this.center = 0.5f * (size - 1);
		this.coeff = new float[size];
	}

	private void normalize() {
		float norm = 0.0f;
		for (int i = 0; i < coeff.length; i++) {
			float c = coeff[i];
			norm += c * c;
		}
		norm = 1.0f / (float) Math.sqrt(norm);
		for (int i = 0; i < coeff.length; i++)
			coeff[i] *= norm;
	}

	/* Construct the filter by combining the impulse response values and the window.
	 * Then normalize to unity gain.
	 */
	private void build(int filter, int window, int size, float samples_per_second) throws IllegalArgumentException {
		init(size);

		for (int i = 0; i < size; i++) {
			float c = coeff(filter, i);
			float s = window(window, size, i, samples_per_second);
			coeff[i] = c * s;
		}
		normalize();
	}

	public AprsFilter convolve(AprsFilter other) {
		int size = length() + other.length () * 2;
		AprsFilter c = new AprsFilter(size);

		for (int i = 0; i < size; i++) {
			int	offset = i - other.length();
			c.coeff[i] = convolve(other.coeff, offset);
		}
		c.normalize();
		return c;
	}

	private float get(int pos) {
		if (pos < 0)
			return 0.0f;
		if (pos >= length())
			return 0.0f;
		return coeff[pos];
	}

	public AprsFilter add(AprsFilter other) {
		AprsFilter c = new AprsFilter(Math.max (length(), other.length()));

		int	off_this = (c.length() - length()) / 2;
		int	off_other = (c.length () - other.length()) / 2;

		for (int i = 0; i < c.length(); i++)
			c.coeff[i] = coeff[i - off_this] + other.coeff[i - off_other];
		c.normalize();
		return c;
	}

	private AprsFilter(int size) {
		init(size);
	}

	public AprsFilter(int filter, int window, int size, float samples_per_second, float freq) throws IllegalArgumentException {

		if (filter == filter_bandpass)
			throw new IllegalArgumentException("one parameter filter cannot be bandpass");

		cps = freq / samples_per_second;

		build(filter, window, size, samples_per_second);
	}

	public AprsFilter(int filter, int window, int size, float samples_per_second, float low, float high) throws IllegalArgumentException {

		if (filter != filter_bandpass)
			throw new IllegalArgumentException("two parameter filter must be bandpass");

		cps_low = low / samples_per_second;
		cps_high = high / samples_per_second;
		/* middle of passband */
		cps = (cps_low + cps_high) / 2.0f;

		build(filter, window, size, samples_per_second);
	}
}
