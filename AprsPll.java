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

public class AprsPll {

	int	samples_per_second;
	float	baud_rate;
	float	clock;
	float	step;
	int	samples;
	int	prev_bit;

	AprsBit	receiver;

	static final float	locked_inertia = 0.73f / 1000;
	static final float	searching_inertia = 0.64f / 1000;

	public void receive(int bit, boolean locked) {
		++samples;
		clock += step;

		if (clock >= 0.5f) {
			clock -= 1.0f;
			receiver.receive(bit);
		}

		if (bit != prev_bit) {
			prev_bit = bit;

			float inertia;
			if (locked)
				inertia = locked_inertia;
			else
				inertia = searching_inertia;

			clock = clock * inertia;
			samples = 0;
		}
	}

	public AprsPll(AprsBit receiver, int samples_per_second, float baud_rate) {
		this.receiver = receiver;
		this.samples_per_second = samples_per_second;
		this.baud_rate = baud_rate;

		step = baud_rate / samples_per_second;
		clock = 0.0f;
		samples = 0;
		prev_bit = 0;
	}
}
