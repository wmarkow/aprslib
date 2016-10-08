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

public class AprsAgc {

	float	attack, decay;
	float	peak, valley;

	public float sample(float in) {
		if (in >= peak)
			peak = in * attack + peak * (1.0f - attack);
		else
			peak = in * decay + peak * (1.0f - decay);

		if (in <= valley)
			valley = in * attack + valley * (1.0f - attack);
		else
			valley = in * decay + valley * (1.0f - decay);

		if (peak <= valley)
			return 0.0f;
		return (in - 0.5f * (peak + valley)) / (peak - valley);
	}

	public AprsAgc(float attack, float decay) {
		this.attack = attack;
		this.decay = decay;
		peak = 0.0f;
		valley = 0.0f;
	}
}
