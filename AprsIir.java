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

package org.altusmetrum.aprslib_1;

public class AprsIir {

	class biquad {
		final float	b0, b1, b2, a1, a2;

		float		w1, w2;

		float filter(float x0) {
			float	accumulator;
			float	w0;

			/* Run feedback part of filter */
			accumulator  = w2 * a2;
			accumulator += w1 * a1;
			accumulator += x0 ;

			w0 = accumulator;

			/* Run feedforward part of filter */
			accumulator  = w0 * b0;
			accumulator += w1 * b1;
			accumulator += w2 * b2;

			w2 = w1;		// Shuffle history buffer
			w1 = w0;

			return accumulator;
		}

		void reset() {
			w1 = w2 = 0;
		}

		biquad(float b0, float b1, float b2, float a1, float a2) {
			this.b0 = b0;
			this.b1 = b1;
			this.b2 = b2;
			this.a1 = a1;
			this.a2 = a2;
		}
	}

	biquad[]	biquads;

	static final float[]	band_coeff = {
		/* Scaled for floating point */
		0.5467085797931144f, -1.0934171595862288f, 0.5467085797931144f, 1.8995374900575972f, -0.9314401473000165f,	// b0, b1, b2, a1, a2
		0.5f, -1f, 0.5f, 1.9595060786696943f, -0.9791438846133993f,							// b0, b1, b2, a1, a2
		0.015625f, 0.03125f, 0.015625f, 1.8330685107526572f, -0.901131762971006f,					// b0, b1, b2, a1, a2
		0.0078125f, 0.015625f, 0.0078125f, 1.8352188343679972f, -0.9504662614217543f					// b0, b1, b2, a1, a2
	};

	static final float[]	lp_coeff = {
		0.026736387616333877f, 0.05347277523266775f, 0.026736387616333877f, 1.502704181197029f, -0.5830614019773526f,// b0, b1, b2, a1, a2
		0.03125f, 0.0625f, 0.03125f, 1.639887396470299f, -0.8070827730925009f// b0, b1, b2, a1, a2
	};

	float filter(float a) {
		for (int i = 0; i < biquads.length; i++)
			a = biquads[i].filter(a);
		return a;
	}

	static final int	filter_bandpass = 1;
	static final int	filter_lowpass = 2;

	AprsIir(int type) {
		float[]	coeff;

		switch (type) {
		case filter_bandpass:
		default:
			coeff = band_coeff;
			break;
		case filter_lowpass:
			coeff = lp_coeff;
			break;
		}

		int	nb = coeff.length / 5;

		biquads = new biquad[nb];
		for (int i = 0; i < nb; i++) {
			int	o = i * 5;
			biquads[i] = new biquad(coeff[o+0],
						coeff[o+1],
						coeff[o+2],
						coeff[o+3],
						coeff[o+4]);
		}
	}

	AprsIir() {
		this(filter_bandpass);
	}
}
