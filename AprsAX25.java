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

public class AprsAX25 implements AprsData {

	static final int SSID_H_MASK = 0x80;
	static final int SSID_H_SHIFT = 7;

	static final int SSID_RR_MASK = 0x60;
	static final int SSID_RR_SHIFT = 5;

	static final int SSID_SSID_MASK = 0x1e;
	static final int SSID_SSID_SHIFT = 1;

	static final int SSID_LAST_MASK = 0x01;

	static final int AX25_MAX_REPEATERS = 8;
	static final int AX25_MIN_ADDRS = 2;
	static final int AX25_MAX_ADDRS = 10;	/* Destination, Source, 8 digipeaters. */

	static final int AX25_DESTINATION = 0;	/* Address positions in frame. */
	static final int AX25_SOURCE =      1;
	static final int AX25_REPEATER_1 = 2;
	static final int AX25_REPEATER_2 = 3;
	static final int AX25_REPEATER_3 = 4;
	static final int AX25_REPEATER_4 = 5;
	static final int AX25_REPEATER_5 = 6;
	static final int AX25_REPEATER_6 = 7;
	static final int AX25_REPEATER_7 = 8;
	static final int AX25_REPEATER_8 = 9;

	static final int AX25_MAX_ADDR_LEN = 12;/* In theory, you would expect the maximum length
						 * to be 6 letters, dash, 2 digits, and nul for a
						 * total of 10.  However, object labels can be 10
						 * characters so throw in a couple extra bytes
						 * to be safe.
						 */

	static final int AX25_MIN_INFO_LEN = 0; /* Previously 1 when considering only APRS. */

	static final int AX25_MAX_INFO_LEN = 2048;	/* Maximum size for APRS.
							 * AX.25 starts out with 256 as the default max
							 * length but the end stations can negotiate
							 * something different.
							 * version 0.8:  Change from 256 to 2028 to
							 * handle the larger paclen for Linux AX25.
							 *
							 *
							 * These don't include the 2 bytes for the
							 * HDLC frame FCS.
							 */

	static final int AX25_MIN_PACKET_LEN = ( 2 * 7 + 1 );
	static final int AX25_MIN_TOTAL_LEN = AX25_MIN_PACKET_LEN + 2;	/* add the CRC */
	static final int AX25_MAX_PACKET_LEN = ( AX25_MAX_ADDRS * 7 + 2 + 3 + AX25_MAX_INFO_LEN);
	static final int AX25_MAX_TOTAL_LEN = AX25_MAX_PACKET_LEN + 2;	/* add the CRC */

	static final int AX25_UI_FRAME = 3;		/* Control field value. */

	/* PID values */
	static final int AX25_PID_ISO_8208		= 0x01;
	static final int AX25_PID_C_TCP			= 0x06;
	static final int AX25_PID_UC_TCP		= 0x07;
	static final int AX25_PID_FRAGMENT		= 0x08;
	static final int AX25_PID_AX25_LAYER_3_MASK	= 0x30;
	static final int AX25_PID_AX25_LAYER_3_VALUE_1	= 0x10;
	static final int AX25_PID_AX25_LAYER_3_VALUE_2	= 0x20;
	static final int AX25_PID_TEXNET		= 0xc3;
	static final int AX25_PID_LINK_QUALITY		= 0xc4;
	static final int AX25_PID_APPLETALK		= 0xca;
	static final int AX25_PID_APPLETALK_ARP		= 0xcb;
	static final int AX25_PID_IP			= 0xcc;
	static final int AX25_PID_ARP			= 0xcd;
	static final int AX25_PID_FLEXNET		= 0xce;
	static final int AX25_PID_NETROM		= 0xcf;
	static final int AX25_PID_NO_LAYER_3 		= 0xf0;
	static final int AX25_PID_ESCAPE		= 0xff;

	byte[]	frame;
	int	total_len;
	int	frame_len;
	int	num_addr;

	AprsPacket	packet;

	private final int frame(int i) {
		return frame[i] & 0xff;
	}

	int num_addr() {
		int num_addr = 0;

		int addr_bytes = 0;
		for (int a = 0; a < frame_len && addr_bytes == 0; a++) {
			if ((frame(a) & SSID_LAST_MASK) != 0) {
				addr_bytes = a + 1;
			}
		}

		if (addr_bytes % 7 == 0) {
			int addrs = addr_bytes / 7;
			if (addrs >= AX25_MIN_ADDRS && addrs <= AX25_MAX_ADDRS) {
				num_addr = addrs;
			}
		}
		return num_addr;
	}

	int ssid(int n) {
		if (n < 0 || n >= num_addr)
			return 0;
		return (frame(6 + n*7) & SSID_SSID_MASK) >> SSID_SSID_SHIFT;
	}

	AprsAddress address(int n) {
		if (n < 0 || n >= num_addr)
			return new AprsAddress("??????", 0);

		StringBuilder s = new StringBuilder();
		for (int i = 0; i < 6; i++) {
			int ch = frame(n*7 + i) >> 1;
			if (ch <= ' ') break;
			if (ch > 'a')
				System.out.printf("weird address %x\n", ch);
			s.appendCodePoint(ch);
		}

		return new AprsAddress(s.toString(), ssid(n));
	}

	boolean repeated(int n) {
		if (n < 0 || n >= num_addr)
			return false;
		return (frame(n*7 + 6) & SSID_H_MASK) != 0;
	}

	int heard() {
		int heard;
		heard = AX25_SOURCE;

		for (int n = AX25_REPEATER_1; n < num_addr; n++) {
			if (repeated(n))
				heard = n;
		}
		return heard;

	}

	int control_offset() {
		return num_addr * 7;
	}

	int num_control() {
		return 1;	/* XXX Always 1 for U frame. I and S are harder */
	}

	int pid_offset() {
		return control_offset() + num_control();
	}

	int num_pid() {
		int c = frame(control_offset());

		if ((c & 0x01) == 0x00 ||		/* I  xxxx xxx0 */
		    (c & 0xef) == 0x03) {		/* UI 000x 0011 */

			int pid = frame(pid_offset());
			if (pid == 0xff)
				return 2;		/* pid 0xff means there's another one */
			return 1;
		}
		return 0;
	}

	int info_offset() {
		return control_offset() + num_control() + num_pid();
	}

	int num_info() {
		int len;

		len = frame_len - control_offset() - num_control() - num_pid();
		if (len < 0)
			return 0;
		return len;
	}

	int pid() {
		if (num_addr >= 2)
			return frame(pid_offset());
		return -1;
	}

	int control() {
		if (num_addr >= 2)
			return frame(control_offset());
		return -1;
	}

	int c2() {
		if (num_addr >= 2)
			return frame(control_offset() + 1);
		return -1;
	}

	String info() {
		int	offset;
		int	len;

		if (num_addr >= 2) {
			offset = info_offset();
			len = num_info();
		} else {
			offset = 0;
			len = frame_len;
		}

		StringBuilder s = new StringBuilder();
		for (int i = 0; i < len; i++)
			s.appendCodePoint(frame(offset + i));
		return s.toString();
	}

	public String toString() {
		if (frame_len == 0)
			return "??????";

		if (num_addr < 2)
			return info();

		AprsAddress	destination = address(0);
		AprsAddress	source = address(1);

		StringBuilder s = new StringBuilder();

		s.append(source.toString());
		s.append('>');
		s.append(destination.toString());
		for (int i = 2; i < num_addr; i++) {
			s.append(',');
			s.append(address(i).toString());
		}
		s.append(':');

		AprsAprs aa = new AprsAprs(this);
		s.append(aa.toString());
		return s.toString();
	}

	/* AprsData interface */

	public void carrier_detect(boolean detect) {
		packet.carrier_detect(detect);
	}

	public void data(byte b) {
		if (total_len < AX25_MAX_TOTAL_LEN)
			frame[total_len++] = b;
	}

	public void start() {
		if (total_len > 0)
			stop();
		total_len = 0;
		frame_len = 0;
		num_addr = 0;
	}

	public void stop() {
		if (total_len < AX25_MIN_TOTAL_LEN)
			return;

		int	transmitted_fcs = frame(total_len - 2) | (frame(total_len - 1) << 8);
		int	computed_fcs = AprsFcs.fcs(frame, total_len - 2);

		if (transmitted_fcs != computed_fcs)
			return;

		frame_len = total_len - 2;
		num_addr = num_addr();

		packet.receive(new AprsAprs(this));
	}

	public AprsAX25(AprsPacket packet) {
		this.packet = packet;
		frame = new byte[AX25_MAX_TOTAL_LEN];
		total_len = 0;
	}
}
