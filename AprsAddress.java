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

public class AprsAddress {
	public String	callsign;
	public int	ssid;

	public String toString() {
		if (ssid == 0)
			return callsign;
		return String.format("%s-%d", callsign, ssid);
	}

	public AprsAddress(String callsign, int ssid) {
		this.callsign = callsign;
		this.ssid = ssid;
	}
}
