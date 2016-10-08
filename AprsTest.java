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

import java.io.*;

class Receive implements AprsData {

	public void carrier_detect(boolean detect) {
		System.out.printf("DCD %b\n", detect);
	}

	public void data(byte b) {
		System.out.printf ("\tbyte %02x\n", b);
	}

	public void start() {
		System.out.printf ("start...\n");
	}

	public void stop() {
		System.out.printf("...stop\n");
	}
}

public class AprsTest {

	public AprsTest() {
	}

	static private int sample(FileInputStream f) throws IOException {
		int	a = f.read();
		if (a == -1)
			return -1;
		int	b = f.read();
		if (b == -1)
			return -1;

		return a | (b << 8);
	}

	static void dump_filter(AprsFilter f) {
		float[]	c = f.coeff;

		for (int i = 0; i < c.length; i++)
			System.out.printf ("%d %g\n", i, c[i]);
	}

	public static void main(final String[] args) {
//		AprsData	data = new Receive();
		AprsData	data = new AprsAX25();
		AprsDemod	demod = new AprsDemod(data);
		boolean		any_read = false;

		for (int i = 0; i < args.length; i++) {
			try {
				FileInputStream f = new FileInputStream(args[i]);
				try {
					for (;;) {
						int input = sample(f);
						if (input == -1)
							break;
						if (input >= 32768)
							input = input - 65536;
						float raw = input / 16384.0f;
						demod.demod(raw);
					}
					any_read = true;
				} catch (IOException ie) {
				}
				try {
					f.close();
				} catch (IOException ie) {
				}
			} catch (FileNotFoundException fe) {
				System.out.printf("No such file %s\n", args[i]);
			}
		}
		if (any_read)
			for (int e = 0; e < 1024; e++)
				demod.demod(0.0f);
	}
}
