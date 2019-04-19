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

package org.altusmetrum.aprslib_1;

public class AprsIir {

	static final float[] numerator =
	{
		-0.00090056609798273412f, /* z^{0} */
		-2.5480311925112531e-19f, /* z^{-1} */
		0.0045028304899136697f, /* z^{-2} */
		-2.0384249540090025e-18f, /* z^{-3} */
		-0.0090056609798273429f, /* z^{-4} */
		-0f, /* z^{-5} */
		0.0090056609798273429f, /* z^{-6} */
		2.0384249540090025e-18f, /* z^{-7} */
		-0.0045028304899136697f, /* z^{-8} */
		2.5480311925112531e-19f, /* z^{-9} */
		0.00090056609798273412f, /* z^{-10} */
	};


	static final float[] denominator =
	{
		1f, /* z^{0} */
		-11.398985386103741f, /* z^{-1} */
		58.865824337335113f, /* z^{-2} */
		-181.32624283693013f, /* z^{-3} */
		368.8806530885679f, /* z^{-4} */
		-517.74114004270632f, /* z^{-5} */
		507.59686688571645f, /* z^{-6} */
		-343.14427481657441f, /* z^{-7} */
		153.02490888106149f, /* z^{-8} */
		-40.634715516555147f, /* z^{-9} */
		4.8771057116521135f, /* z^{-10} */
	};

	AprsRing	input;
	AprsRing	output;

	float filter(float a) {
		input.put(a);

		float v = 0;
		for (int i = 0; i < 11; i++)
			v -= input.get(i) * denominator[i];

		for (int i = 0; i < 10; i++)
			v += output.get(i) * numerator[i+1];
		output.put(v);
		return v;
	}

	AprsIir() {
		input = new AprsRing(11);
		output = new AprsRing(11);
	}
}
