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

public class AprsDemod {

	static int round(float f) { return (int) Math.round(f); }

	static final float	baud_rate = 1200.0f;
	static final float	mark_freq = 1200.0f;
	static final float	space_freq = 2200.0f;

	static final float	agc_attack = 0.130f;
	static final float	agc_decay = 0.00013f;

	static final float	hysteresis = 0.01f;

	AprsRing	input_ring;

	AprsGoertzel	mark_g;
	AprsGoertzel	space_g;

	AprsAgc		mark_agc;
	AprsAgc		space_agc;

	static final float	pre_filter_baud = 0.23f;

	AprsFilter	pre_filter;
	AprsFilter	mark_cos_filter;
	AprsFilter	mark_sin_filter;
	AprsFilter	space_cos_filter;
	AprsFilter	space_sin_filter;
	AprsFilter	low_filter;

	AprsRing	pre_ring, mark_ring, space_ring;

	AprsPll		pll;
	AprsHdlc	hdlc;

	int		demod_prev = 0;

	private float z(float a, float b) {
		return (float) Math.hypot(a,b);
	}

	int count = 0;

	boolean	expensive = true;
	boolean prefilter = true;

	public void demod(float input) {

		if (prefilter) {
			pre_ring.put(input);
			input = pre_filter.convolve(pre_ring);
		}

		input_ring.put(input);

		float mark_raw, space_raw;

		if (expensive) {
			mark_ring.put(z(mark_cos_filter.convolve(input_ring),
					mark_sin_filter.convolve(input_ring)));
			space_ring.put(z(space_cos_filter.convolve(input_ring),
					 space_sin_filter.convolve(input_ring)));
			mark_raw  = low_filter.convolve(mark_ring);
			space_raw = low_filter.convolve(space_ring);
		} else {

			mark_raw  = mark_g.filter(input_ring);
			space_raw = space_g.filter(input_ring);
		}

		/* AGC mark and space separately */
		float mark_value = mark_agc.sample(mark_raw);
		float space_value = space_agc.sample(space_raw);

		/* see which is bigger */
		float demod_out = mark_value - space_value;

		int demod_val = demod_prev;

		/* Avoid jitter around zero by adding a bit of hysteresis */
		if (demod_out > hysteresis)
			demod_val = 1;
		else if (demod_out < -hysteresis)
			demod_val = 0;

		/* Pass along to the PLL to detect bits */
		pll.receive(demod_val, false);
	}

	public void flush() {
		int	len = (input_ring.data.length + 1) / 2;

		for (int i = 0; i < len; i++)
			demod(0.0f);
	}

	public AprsDemod(AprsData data, float sample_rate, int len) {

		if (prefilter) {
			int	pre_filter_len = 128 * 3;

			pre_filter = new AprsFilter(AprsFilter.filter_bandpass,
						    AprsFilter.window_truncated,
						    pre_filter_len,
						    sample_rate,
						    Math.min(mark_freq, space_freq) - pre_filter_baud * baud_rate,
						    Math.max(mark_freq, space_freq) + pre_filter_baud * baud_rate);

			pre_ring = new AprsRing(pre_filter_len);
		}

		if (expensive) {
			int	sample_filter_len = 25 * 3;
			int	low_filter_len = 21 * 3;

			mark_cos_filter = new AprsFilter(AprsFilter.filter_cos,
							 AprsFilter.window_cosine,
							 sample_filter_len,
							 sample_rate,
							 mark_freq);
			mark_sin_filter = new AprsFilter(AprsFilter.filter_sin,
							 AprsFilter.window_cosine,
							 sample_filter_len,
							 sample_rate,
							 mark_freq);
			space_cos_filter = new AprsFilter(AprsFilter.filter_cos,
							  AprsFilter.window_cosine,
							  sample_filter_len,
							  sample_rate,
							  space_freq);
			space_sin_filter = new AprsFilter(AprsFilter.filter_sin,
							  AprsFilter.window_cosine,
							  sample_filter_len,
							  sample_rate,
							  space_freq);

			low_filter = new AprsFilter(AprsFilter.filter_lowpass,
						    AprsFilter.window_truncated,
						    low_filter_len,
						    sample_rate,
						    1.16f * baud_rate);

			input_ring = new AprsRing(sample_filter_len);
			mark_ring = new AprsRing(low_filter_len);
			space_ring = new AprsRing(low_filter_len);

		} else {
			mark_g = new AprsGoertzel(sample_rate, mark_freq, len);
			space_g = new AprsGoertzel(sample_rate, space_freq, len);

			input_ring = new AprsRing(len);
		}

		mark_agc = new AprsAgc(agc_attack, agc_decay);
		space_agc = new AprsAgc(agc_attack, agc_decay);

		hdlc = new AprsHdlc(data);
		pll = new AprsPll(hdlc, sample_rate, baud_rate);
	}

	public AprsDemod(AprsPacket packet, float sample_rate, int len) {
		this(new AprsAX25(packet), sample_rate, len);
	}

	public AprsDemod(AprsPacket packet, float sample_rate) {
		this(packet, sample_rate, 75);
	}
}
