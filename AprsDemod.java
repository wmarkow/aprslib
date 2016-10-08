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
	static final int	sample_rate = 44100;
	static final float	pre_filter_baud = 0.23f;

	static final float	agc_attack = 0.130f;
	static final float	agc_decay = 0.00013f;

	static final float	hysteresis = 0.01f;

	static final float	pre_filter_len_bits = 128 * baud_rate / (sample_rate / 3);
	static final float	sample_filter_len_bits = 25 * baud_rate / (sample_rate / 3);
	static final float	low_filter_len_bits = 21 * baud_rate / (sample_rate / 3);

	static final int	pre_filter_size = round(pre_filter_len_bits * sample_rate / baud_rate);
	static final int	sample_filter_size = round(sample_filter_len_bits * sample_rate / baud_rate);
	static final int	low_filter_size = round(low_filter_len_bits * sample_rate / baud_rate);

	static final AprsFilter pre_filter = new AprsFilter(AprsFilter.filter_bandpass,
							    AprsDsp.window_truncated,
							    pre_filter_size,
							    sample_rate,
							    Math.min(mark_freq, space_freq) - pre_filter_baud * baud_rate,
							    Math.max(mark_freq, space_freq) + pre_filter_baud * baud_rate);

	static final AprsFilter	mark_cos_filter = new AprsFilter(AprsFilter.filter_cos,
								 AprsDsp.window_cosine,
								 sample_filter_size,
								 sample_rate,
								 mark_freq);
	static final AprsFilter	mark_sin_filter = new AprsFilter(AprsFilter.filter_sin,
								 AprsDsp.window_cosine,
								 sample_filter_size,
								 sample_rate,
								 mark_freq);
	static final AprsFilter	space_cos_filter = new AprsFilter(AprsFilter.filter_cos,
								 AprsDsp.window_cosine,
								 sample_filter_size,
								 sample_rate,
								 space_freq);
	static final AprsFilter	space_sin_filter = new AprsFilter(AprsFilter.filter_sin,
								 AprsDsp.window_cosine,
								 sample_filter_size,
								 sample_rate,
								 space_freq);

	static final AprsFilter low_filter = new AprsFilter(AprsFilter.filter_lowpass,
							    AprsDsp.window_truncated,
							    low_filter_size,
							    sample_rate,
							    1.16f * baud_rate);

	static final AprsAgc mark_agc = new AprsAgc(agc_attack, agc_decay);
	static final AprsAgc space_agc = new AprsAgc(agc_attack, agc_decay);

	AprsRing	input_ring = new AprsRing(pre_filter_size);
	AprsRing	sample_ring = new AprsRing(sample_filter_size);
	AprsRing	mark_ring = new AprsRing(low_filter_size);
	AprsRing	space_ring = new AprsRing(low_filter_size);

	int		demod_prev = 0;

	private float z(float a, float b) {
		return (float) Math.hypot(a, b);
	}

	AprsPll		pll;
	AprsHdlc	hdlc;

	float	input;
	float	sample;
	float	mark_raw, space_raw;
	float	mark_low, space_low;
	float	mark_value, space_value;
	float	demod_out;
	int	demod_val;

	float	input_delay;
	float	sample_delay;
	float	raw_delay;
	float	low_delay;
	float	value_delay;
	float	demod_out_delay;
	float	demod_val_delay;

	public void demod(float input) {

		/* input */

		/* Bandpass pre filter */
		this.input = input;
		input_ring.put(input);

		/* sample */

		sample = pre_filter.convolve(input_ring);
		sample_ring.put(sample);

		/* raw */

		mark_raw = z(mark_cos_filter.convolve(sample_ring),
			     mark_sin_filter.convolve(sample_ring));
		mark_ring.put(mark_raw);

		space_raw = z(space_cos_filter.convolve(sample_ring),
			      space_sin_filter.convolve(sample_ring));
		space_ring.put(space_raw);

		/* low */
		mark_low = low_filter.convolve(mark_ring);

		space_low = low_filter.convolve(space_ring);

		/* value */
		mark_value = mark_agc.sample(mark_low);
		space_value = space_agc.sample(space_low);

		/* demod */
		demod_out = mark_value - space_value;

		demod_val = demod_prev;

		if (demod_out > hysteresis)
			demod_val = 1;
		else if (demod_out < -hysteresis)
			demod_val = 0;

		pll.receive(demod_val, false);
	}

	public AprsDemod(AprsData data) {
		hdlc = new AprsHdlc(data);
		pll = new AprsPll(hdlc, sample_rate, baud_rate);

		float	input_to_sample = pre_filter.center;
		float	sample_to_raw = mark_cos_filter.center;
		float	raw_to_low = low_filter.center;
		float	low_to_value = 0.0f;
		float	value_to_demod = 0.0f;

		demod_out_delay = demod_val_delay = 0.0f;
		value_delay = demod_out_delay + value_to_demod;
		low_delay = value_delay + low_to_value;
		raw_delay = low_delay + raw_to_low;
		sample_delay = raw_delay + sample_to_raw;
		input_delay = sample_delay + input_to_sample;
	}
}
