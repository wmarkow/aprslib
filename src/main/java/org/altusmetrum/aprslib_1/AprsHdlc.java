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

public class AprsHdlc implements AprsBit {
	int		prev_raw;
	int		frame_pattern;
	int		carrier_pattern;
	int		octet;
	int		octet_len;	/* -1 means not in a packet */
	boolean		data_detect;
	AprsData	data;

	/* Receive a single bit, run the raw state machine and send
	 * decoded bytes along
	 */
	public void receive(int raw) {
		int bit;

		/* Decode NRZI */
		if (raw == prev_raw)
			bit = 1;
		else {
			prev_raw = raw;
			bit = 0;
		}

		System.out.print(bit);
		/* Run two raw bit-level pattern detectors */

		frame_pattern >>= 1;
		carrier_pattern >>= 1;

		if (bit == 1) {
			frame_pattern |= 0x80;
			carrier_pattern |= 0x800000;
		}

		/* Carrier detect -- either 0 0 frame or frame frame frame */
		if (carrier_pattern == 0x7e7e7e || carrier_pattern == 0x7e0000) {
			if (!data_detect) {
				data_detect = true;
				data.carrier_detect(true);
			}
		}

		/* 9 constant bits, no more data */
		if (frame_pattern == 0xff) {
			if (data_detect) {
				data_detect = false;
				data.carrier_detect(false);
			}
		}

		/* When we see the framing pattern, start decoding bytes */
		if (frame_pattern == 0x7e) {
			System.out.println(" Flag decoded");
			data.start();
			octet_len = 0;
		} else if (frame_pattern == 0xfe) {
			System.out.println(" 0xFE decoded");
			data.stop();
			octet_len = -1;
		} else if ((frame_pattern & 0xfc) == 0x7c) {
			/* Bit stuffing, drop the following zero */
			System.out.println(" Bit stuffing detected");
			;
		} else 	if (octet_len >= 0) {
			octet >>= 1;
			if (bit == 1)
				octet |= 0x80;
			octet_len++;
			if (octet_len == 8) {
				octet_len = 0;
				data.data((byte) octet);
				
				System.out.println(String.format(" Decoded byte: %s", octet));
			}
		}
	}

	public boolean in_frame() {
		return octet_len >= 0;
	}

	public AprsHdlc(AprsData data) {
		this.data = data;

		prev_raw = 0;
		frame_pattern = 0;
		carrier_pattern = 0;
		data_detect = false;
		octet_len = -1;
		octet = 0;
	}
}
