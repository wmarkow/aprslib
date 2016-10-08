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

import java.util.*;
import java.io.*;

public class AprsRing {

	public float[]	data;
	public int	pos;
	boolean		zero;

	public float get(int o) {
		if (zero)
			return data[0];
		return data[(pos + o) % data.length];
	}

	public float get() {
		return get(data.length - 1);
	}

	public void put(float d) {
		data[pos] = d;
		pos = (pos + 1) % data.length;
	}

	public AprsRing(int size) {
		zero = (size == 0);
		if (zero)
			size = 1;
		data = new float[size];
		for (int i = 0; i < size; i++)
			data[i] = 0;
		pos = 0;
	}

	public AprsRing(float delay) {
		this((int) Math.floor(delay));
	}
}
