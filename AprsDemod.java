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
	static final float	pre_filter_baud = 0.23f;

	static final float	agc_attack = 0.130f;
	static final float	agc_decay = 0.00013f;

	static final float	hysteresis = 0.01f;

	float		sample_rate;

	AprsFilter	pre_filter;

	AprsFilter	mark_cos_filter;
	AprsFilter	mark_sin_filter;
	AprsFilter	space_cos_filter;
	AprsFilter	space_sin_filter;

	AprsFilter	low_filter;

	AprsAgc		mark_agc;
	AprsAgc		space_agc;

	AprsRing	input_ring;
	AprsRing	sample_ring;
	AprsRing	mark_ring;
	AprsRing	space_ring;

	AprsPll		pll;
	AprsHdlc	hdlc;

	int		demod_prev = 0;

	private float z(float a, float b) {
		return (float) Math.hypot(a, b);
	}

	public void demod(float input) {

		input_ring.put(input);

		/* Bandpass pre filter */
		float sample = pre_filter.convolve(input_ring);
		sample_ring.put(sample);

		/* Detect mark and space */

		float mark_raw = z(mark_cos_filter.convolve(sample_ring),
				   mark_sin_filter.convolve(sample_ring));
		mark_ring.put(mark_raw);

		float space_raw = z(space_cos_filter.convolve(sample_ring),
				    space_sin_filter.convolve(sample_ring));
		space_ring.put(space_raw);

		/* Low pass */
		float mark_low = low_filter.convolve(mark_ring);

		float space_low = low_filter.convolve(space_ring);

		/* AGC mark and space separately */
		float mark_value = mark_agc.sample(mark_low);
		float space_value = space_agc.sample(space_low);

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
		int	len = (input_ring.data.length + sample_ring.data.length + mark_ring.data.length) / 2;

		for (int i = 0; i < len; i++)
			demod(0.0f);
	}

	private void init() {
		float pre_filter_len_bits = 128 * baud_rate / (sample_rate / 3);
		float sample_filter_len_bits = 25 * baud_rate / (sample_rate / 3);
		float low_filter_len_bits = 21 * baud_rate / (sample_rate / 3);

		int pre_filter_size = round(pre_filter_len_bits * sample_rate / baud_rate);
		int sample_filter_size = round(sample_filter_len_bits * sample_rate / baud_rate);
		int low_filter_size = round(low_filter_len_bits * sample_rate / baud_rate);

		pre_filter = new AprsFilter(AprsFilter.filter_bandpass,
					    AprsDsp.window_truncated,
					    pre_filter_size,
					    sample_rate,
					    Math.min(mark_freq, space_freq) - pre_filter_baud * baud_rate,
					    Math.max(mark_freq, space_freq) + pre_filter_baud * baud_rate);

		mark_cos_filter = new AprsFilter(AprsFilter.filter_cos,
						 AprsDsp.window_cosine,
						 sample_filter_size,
						 sample_rate,
						 mark_freq);
		mark_sin_filter = new AprsFilter(AprsFilter.filter_sin,
						 AprsDsp.window_cosine,
						 sample_filter_size,
						 sample_rate,
						 mark_freq);
		space_cos_filter = new AprsFilter(AprsFilter.filter_cos,
						  AprsDsp.window_cosine,
						  sample_filter_size,
						  sample_rate,
						  space_freq);
		space_sin_filter = new AprsFilter(AprsFilter.filter_sin,
						  AprsDsp.window_cosine,
						  sample_filter_size,
						  sample_rate,
						  space_freq);

		low_filter = new AprsFilter(AprsFilter.filter_lowpass,
					    AprsDsp.window_truncated,
					    low_filter_size,
					    sample_rate,
					    1.16f * baud_rate);

		mark_agc = new AprsAgc(agc_attack, agc_decay);
		space_agc = new AprsAgc(agc_attack, agc_decay);
		input_ring = new AprsRing(pre_filter_size);
		sample_ring = new AprsRing(sample_filter_size);
		mark_ring = new AprsRing(low_filter_size);
		space_ring = new AprsRing(low_filter_size);
	}

	public AprsDemod(AprsData data, float sample_rate) {
		this.sample_rate = sample_rate;

		init ();

		hdlc = new AprsHdlc(data);
		pll = new AprsPll(hdlc, sample_rate, baud_rate);
	}

	public AprsDemod(AprsPacket packet, float sample_rate) {
		this(new AprsAX25(packet), sample_rate);
	}
}
