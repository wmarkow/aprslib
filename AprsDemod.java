/*
 * Copyright © 2016 Keith Packard <keithp@keithp.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
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
 * project is also licensed under the GPL, either version 3 of the
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

	static final float	pre_filter_baud = 0.23f;

	/*
	 * Demodulation options. All of these improve the demodulation
	 * accuracy while increasing the cost of the computation.
	 */

	boolean preiir;			/* Enable iir prefilter */
	boolean prefilter;		/* Enable bandpass prefilter */
	boolean	convolution;		/* Select convolution detector instead of Goertzel */
	boolean lowfilter;		/* Enable lowpass postfilter */

	static final boolean default_preiir = false;
	static final boolean default_prefilter = true;
	static final boolean default_convolution = false;
	static final boolean default_lowfilter = true;

	/* Bandpass prefilter. This can use more samples
	 * than the detectors and hence filter out noise
	 * better
	 */
	AprsIir		pre_iir;
	AprsFilter	pre_filter;
	AprsRing	pre_ring;

	/* Input samples to the detectors */
	AprsRing	input_ring;

	/* Goertzel detectors which operate as a single-bin FFT */
	AprsGoertzel	mark_g;
	AprsGoertzel	space_g;

	/*
	 * Convolution detectors, each of which is a pair of sin waves
	 * 90° out of phase.
	 */
	AprsFilter	mark_cos_filter;
	AprsFilter	mark_sin_filter;
	AprsFilter	space_cos_filter;
	AprsFilter	space_sin_filter;

	/* Low pass postfilter to smooth out the detector results */
	AprsFilter	low_filter;
	AprsRing	mark_ring, space_ring;

	/* Automatic gain control, one for each band to compensate for
	 * different audio paths
	 */
	AprsAgc		mark_agc;
	AprsAgc		space_agc;

	/*
	 * PLL used to track the clock by slowly skewing the baud rate
	 * and phase by watching for zero crossings of the detector
	 */
	AprsPll		pll;

	/*
	 * The output of the PLL feeds into the packet decoder
	 */
	AprsHdlc	hdlc;

	/*
	 * Apply a bit of hysteresis to the decoded bits to reduce
	 * noise at zero crossings of the detector
	 */
	int		demod_prev = 0;

	private float z(float a, float b) {
		return (float) Math.hypot(a,b);
	}

//	int		count = 0;

	public void demod(float input) {

		/* Prefilter with a bandpass to reduce noise outside of the
		 * audio range
		 */
		if (prefilter) {
			pre_ring.put(input);
			input = pre_filter.convolve(pre_ring);
		}
		if (preiir) {
			input = pre_iir.filter(input);
		}

		input_ring.put(input);

		float mark, space;

		if (convolution) {
			mark  = z(mark_cos_filter.convolve(input_ring),
				  mark_sin_filter.convolve(input_ring));
			space = z(space_cos_filter.convolve(input_ring),
				  space_sin_filter.convolve(input_ring));
		} else {
			mark  = mark_g.filter(input_ring);
			space = space_g.filter(input_ring);
		}

		if (lowfilter) {
			mark_ring.put(mark);
			space_ring.put(space);

			mark  = low_filter.convolve(mark_ring);
			space = low_filter.convolve(space_ring);
		}

		/* AGC mark and space separately */
		mark  = mark_agc.sample(mark);
		space = space_agc.sample(space);

		/* see which is bigger */
		float demod_out = mark - space;

		int demod_val = demod_prev;

		/* Avoid jitter around zero by adding a bit of hysteresis */
		if (demod_out > hysteresis)
			demod_val = 1;
		else if (demod_out < -hysteresis)
			demod_val = 0;

//		System.out.printf ("%d %f %f %d\n", ++count, mark, space, demod_val);
		/* Pass along to the PLL to detect bits */
		pll.receive(demod_val, hdlc.in_frame());
	}

	public float baud_rate() {
		return pll.baud_rate();
	}

	public void flush() {
		int	flush = 0;

		if (prefilter)
			flush += (pre_ring.data.length + 1) / 2;

		flush += (input_ring.data.length + 1) / 2;

		if (lowfilter)
			flush += (mark_ring.data.length + 1) / 2;

		for (int i = 0; i < flush; i++)
			demod(0.0f);
	}

	public AprsDemod(AprsData data, float sample_rate, boolean prefilter, boolean convolution, boolean lowfilter, boolean preiir) {
		this.preiir = preiir;
		this.prefilter = prefilter;
		this.convolution = convolution;
		this.lowfilter = lowfilter;

		int	sample_filter_len;

		if (prefilter) {
			float	half_band = baud_rate * pre_filter_baud;
			int	pre_filter_len = 64 * 3;

			pre_filter = new AprsFilter(AprsFilter.filter_bandpass,
						    AprsFilter.window_kaiser,
						    pre_filter_len,
						    sample_rate,
						    Math.min(mark_freq, space_freq) - half_band,
						    Math.max(mark_freq, space_freq) + half_band);

			pre_ring = new AprsRing(pre_filter.length());
		}

		if (convolution) {
			sample_filter_len = 25 * 3;

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
		} else {
			sample_filter_len = 77;

			mark_g = new AprsGoertzel(sample_rate, mark_freq, sample_filter_len);
			space_g = new AprsGoertzel(sample_rate, space_freq, sample_filter_len);
		}

		input_ring = new AprsRing(sample_filter_len);

		if (lowfilter) {
			int	low_filter_len = 21 * 3;

			low_filter = new AprsFilter(AprsFilter.filter_lowpass,
						    AprsFilter.window_kaiser,
						    low_filter_len,
						    sample_rate,
						    1.16f * baud_rate);

			mark_ring = new AprsRing(low_filter_len);
			space_ring = new AprsRing(low_filter_len);
		}

		mark_agc = new AprsAgc(agc_attack, agc_decay);
		space_agc = new AprsAgc(agc_attack, agc_decay);

		hdlc = new AprsHdlc(data);
		pll = new AprsPll(hdlc, sample_rate, baud_rate);
	}

	public AprsDemod(AprsData data, float sample_rate) {
		this(data, sample_rate, default_prefilter, default_convolution, default_lowfilter, default_preiir);
	}

	public AprsDemod(AprsPacket packet, float sample_rate, boolean prefilter, boolean convolution, boolean lowfilter, boolean preiir) {
		this(new AprsAX25(packet), sample_rate, prefilter, convolution, lowfilter, preiir);
	}

	public AprsDemod(AprsPacket packet, float sample_rate) {
		this(new AprsAX25(packet), sample_rate);
	}
}
