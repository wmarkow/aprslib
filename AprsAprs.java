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
import java.util.regex.*;

class position {
	char	dti;
	String	lat;
	char	sym_table_id;
	String	lon;
	char	symbol_code;
	String	comment;

	double latitude() {
		return new latitude_8(lat).value();
	}

	double longitude() {
		return new longitude_9(lon).value();
	}

	public position() {
	}

	public position(String info) {
		dti = info.charAt(0);			/* 0 */
		lat = info.substring(1, 1+8);		/* 1 - 8 */
		sym_table_id = info.charAt(9);		/* 9 */
		lon = info.substring(10, 10 + 9);	/* 10 - 18 */
		symbol_code = info.charAt(19);		/* 19 */
		comment = info.substring(20);		/* 20 - */
	}
}

class compressed_position {
	char	dti;
	char	sym_table_id;
	String	y;
	String	x;
	char	symbol_code;
	char	c;
	char	s;
	char	t;
	String	comment;

	double latitude () {
		return base91.latitude(y);
	}

	double longitude() {
		return base91.longitude(x);
	}

	char symbol_table() {
		if (sym_table_id == '/' || sym_table_id == '\\' || Character.isUpperCase(sym_table_id))
			return sym_table_id;
		else if ('a' <= sym_table_id && sym_table_id <= 'j')
			return (char) (sym_table_id - 'a' + '0');
		else
			return '/';
	}

	public compressed_position() {
	}

	public compressed_position(String info) {
		dti = info.charAt(0);			/* 0 */
		sym_table_id = info.charAt(1);		/* 1 */
		y = info.substring(2, 2+4);		/* 2 - 5 */
		x = info.substring(6, 6+4);		/* 6 - 9 */
		symbol_code = info.charAt(10);		/* 10 */
		c = info.charAt(11);			/* 11 */
		s = info.charAt(12);			/* 12 */
		t = info.charAt(13);			/* 13 */
		comment = info.substring(14);		/* 14 - */
	}
}

class position_time extends position {
	String	time_stamp;

	public position_time(String info) {
		dti = info.charAt(0);			/* 0 */
		time_stamp = info.substring(1, 1+7);	/* 1 - 7 */
		lat = info.substring(8, 8+8);		/* 8 - 15 */
		sym_table_id = info.charAt(16);		/* 16 */
		lon = info.substring(17, 17 + 9);	/* 17 - 25 */
		symbol_code = info.charAt(26);		/* 26 */
		comment = info.substring(27);		/* 27 - */
	}
}

class compressed_position_time extends compressed_position {
	String	time_stamp;

	public compressed_position_time() {
	}

	public compressed_position_time(String info) {
		dti = info.charAt(0);			/* 0 */
		time_stamp = info.substring(1, 1+7);	/* 1 - 7 */
		sym_table_id = info.charAt(8);		/* 8 */
		y = info.substring(9, 9+4);		/* 9 - 12 */
		x = info.substring(13, 13+4);		/* 13 - 16 */
		symbol_code = info.charAt(17);		/* 17 */
		c = info.charAt(18);			/* 18 */
		s = info.charAt(19);			/* 19 */
		t = info.charAt(20);			/* 20 */
		comment = info.substring(21);		/* 21 */
	}
}

class latitude_8 {
	String	deg;
	String	minn;
	char dot;
	String	hmin;
	char ns;

	double value() {

		double result = 0;

		result += Integer.parseInt(deg);
		result += Integer.parseInt(minn) / 60.0;
		result += Integer.parseInt(hmin) / 60.0 / 100;

		switch (ns) {
		case 'n':
		case 'N':
			break;
		case 's':
		case 'S':
			result = -result;
		}
		return result;
	}

	public latitude_8(String value) {
		deg = value.substring(0, 0 + 2);	/* 0 - 1 */
		minn = value.substring(2, 2 + 2);	/* 2 - 3 */
		dot = value.charAt(4);			/* 4 */
		hmin = value.substring(5, 5 + 2);	/* 5 - 6 */
		ns = value.charAt(7);			/* 7 */
	}
}

class longitude_9 {
	String	deg;
	String	minn;
	char dot;
	String	hmin;
	char ew;

	double value() {

		double result = 0;

		result += Integer.parseInt(deg);
		result += Integer.parseInt(minn) / 60.0;
		result += Integer.parseInt(hmin) / 60.0 / 100;

		switch (ew) {
		case 'e':
		case 'E':
			break;
		case 'w':
		case 'W':
			result = -result;
		}
		return result;
	}

	public longitude_9(String value) {
		deg = value.substring(0, 0 + 3);	/* 0 - 2 */
		minn = value.substring(3, 3 + 2);	/* 3 - 4 */
		dot = value.charAt(5);			/* 5 */
		hmin = value.substring(6, 6 + 2);	/* 6 - 7 */
		ew = value.charAt(8);			/* 8 */
	}
}

class base91 {
	static final char b91_min = '!';
	static final char b91_max = '{';

	static boolean is_b91(char c) {
		return b91_min <= c && c <= b91_max;
	}

	static int decode(String v) {
		return ((v.charAt(0)-33) * 91 * 91 * 91 +
			(v.charAt(1)-33) * 91 * 91 +
			(v.charAt(2)-33) * 91) +
			(v.charAt(3));
	}

	static double latitude (String y) {
		return 90 - decode(y)/380926.0;
	}

	static double longitude(String x) {
		return -180 + decode(x)/190463.0;
	}

}

class mic_e {
	char	dti;
	String	lon;
	String	speed_course;
	char	symbol_code;
	char	sym_table_id;
	String	status;

	int std_msg = 0;
	int cust_msg = 0;

	static final String[] std_text = {
		"Emergency", "Priority", "Special", "Committed", "Returning", "In Service", "En Route", "Off Duty"
	};
	static final String[] cust_text = {
		"Emergency", "Custom-6", "Custom-5", "Custom-4", "Custom-3", "Custom-2", "Custom-1", "Custom-0"
	}; 

	String std_text() {
		return std_text[std_msg];
	}

	String cust_text() {
		return cust_text[cust_msg];
	}

	int digit(int c, int mask) {
		if ('0' <= c && c <= '9')
			return c - '0';
		if ('A' <= c && c <= 'J') {
			cust_msg |= mask;
			return c - 'A';
		}
		if ('P' <= c && c <= 'Y') {
			std_msg |= mask;
			return c - 'P';
		}

		if (c == 'K') {
			cust_msg |= mask;
			return 0;
		}

		if (c == 'L')
			return 0;

		if (c == 'Z') {
			std_msg |= mask;
			return 0;
		}
		return 0;
	}

	public mic_e(String info) {
		dti = info.charAt(0);			/* 0 */
		lon = info.substring(1, 1+3);		/* 1 - 3 */
		speed_course = info.substring(4, 4+3);	/* 4 - 6 */
		symbol_code = info.charAt(7);		/* 7 */
		sym_table_id = info.charAt(8);		/* 8 */
		status = info.substring(9);		/* 9 - */
	}
};

class object {
	char	dti;
	String	name;
	char	live;
	String	time;
	String	lat;
	char	sym_table_id;
	String	lon;
	char	symbol_code;
	String	course_speed;
	String	comment;

	double latitude() {
		return new latitude_8(lat).value();
	}

	double longitude() {
		return new longitude_9(lon).value();
	}

	public object(String info) {
		dti = info.charAt(0);			/* 0 */
		name = info.substring(1, 1+9);		/* 1 - 9 */
		live = info.charAt(10);			/* 10 */
		time = info.substring(11,11+7);		/* 11 - 17 */
		lat = info.substring(18, 18+8);		/* 18 - 25 */
		sym_table_id = info.charAt(26);		/* 26 */
		lon = info.substring(27, 27+9);		/* 27 - 35 */
		symbol_code = info.charAt(36);		/* 36 */
		course_speed = info.substring(37, 37+7);/* 37 - 43 */
		comment = info.substring(44);		/* 44 - */
	}
}

class compressed_object extends compressed_position_time {
	String	name;
	char	live;

	public compressed_object(String info) {
		dti = info.charAt(0);			/* 0 */
		name = info.substring(1, 1+9);		/* 1 - 9 */
		live = info.charAt(10);			/* 10 */
		time_stamp = info.substring(11,11+7);		/* 11 - 17 */
		sym_table_id = info.charAt(18);		/* 18 */
		y = info.substring(19, 19+4);		/* 19 - 22 */
		x = info.substring(23, 23+4);		/* 23 - 26 */
		symbol_code = info.charAt(27);		/* 27 */
		c = info.charAt(28);			/* 28 */
		s = info.charAt(29);			/* 29 */
		t = info.charAt(30);			/* 30 */
		comment = info.substring(31);		/* 31 */
	}
}

class message {
	char	dti;
	String	addressee;
	char	colon;
	String	message;

	public message(String info) {
		dti = info.charAt(0);			/* 0 */
		addressee = info.substring(1, 1+9);	/* 1-9 */
		colon = info.charAt(10);		/* 10 */
		message = info.substring(11);		/* 11 - */
	}
}

public class AprsAprs {

	static final int type_mic_e		= 0x1c;
	static final int type_old_mic_e_rev_0	= 0x1d;
	static final int type_position		= '!';
	static final int type_peet_bros		= '#';
	static final int type_raw_gps		= '$';
	static final int type_agrelo		= '%';
	static final int type_old_mic_e		= '\'';
	static final int type_item		= ')';
	static final int type_peet_bros_alt	= '*';
	static final int type_test_data		= ',';
	static final int type_position_time	= '/';
	static final int type_message		= ':';
	static final int type_object		= ';';
	static final int type_station_cap	= '<';
	static final int type_position_msg	= '=';
	static final int type_status		= '>';
	static final int type_query		= '?';
	static final int type_position_time_msg	= '@';
	static final int type_telemetry		= 'T';
	static final int type_maidenhead_grid	= '[';
	static final int type_weather		= '_';
	static final int type_current_mic_e	= '`';
	static final int type_user_defined	= '{';
	static final int type_third_party	= '}';

	static public final int	UNKNOWN = 0x7fffffff;

	public String		info;
	public AprsAddress	source;
	public AprsAddress	destination;
	public String		message_type;
	public char		symbol_table = '/';
	public char		symbol_code = ' ';
	public double		latitude = UNKNOWN;
	public double		longitude = UNKNOWN;
	public String		name;
	public String		addressee;
	public String		message;
	public double		speed_mph = UNKNOWN;
	public double		course = UNKNOWN;
	public int		power = UNKNOWN;
	public int		antenna_height = UNKNOWN;
	public int		antenna_gain = UNKNOWN;
	public String		directivity;
	public double		range = UNKNOWN;
	public double		altitude_ft = UNKNOWN;
	public String		manufacturer;
	public String		mic_e_status;
	public double		frequency = UNKNOWN;
	public double		ctcss_tone = UNKNOWN;
	public int		dcs = UNKNOWN;
	public int		offset_khz = UNKNOWN;
	public String		query_type;
	public double		footprint_lat = UNKNOWN;
	public double		footprint_lon = UNKNOWN;
	public double		footprint_radius = UNKNOWN;
	public String		query_callsign;
	public String		weather;
	public String		telemetry;
	public String		comment;

	private int hex_char(Reader r) throws IOException {
		int	c = r.read();

		if ('0' <= c && c <= '9')
			return c - '0';
		if ('a' <= c && c <= 'f')
			return c - 'a' + 10;
		if ('A' <= c && c <= 'F')
			return c - 'A' + 10;
		if (c == '-')
			return -1;
		throw new IOException();
	}

	int	hex_read;

	private int hex_short(Reader r) throws IOException {
		int a = hex_char(r);
		int b = hex_char(r);
		int c = hex_char(r);
		int d = hex_char(r);

		hex_read++;

		if (a == -1 || b == -1 || c == -1 || d == -1)
			return UNKNOWN;
		return (a << 12) | (b << 8) | (c << 4) | (d << 0);
	}

	private void skip(Reader r, int i) throws IOException {
		for (int j = 0; j < i; j++)
			r.read();
	}

	private double km_to_miles(double km) {
		if (km == UNKNOWN)
			return UNKNOWN;
		return km * 0.621371192;
	}

	private double knots_to_mph(double knots) {
		if (knots == UNKNOWN)
			return UNKNOWN;
		return knots * 1.15077945;
	}

	private double m_to_ft(double m) {
		if (m == UNKNOWN)
			return UNKNOWN;
		return m * (100 / 2.54 / 12);
	}

	private double mbar_to_inhg(double mbar) {
		if (mbar == UNKNOWN)
			return UNKNOWN;
		return mbar * 0.0295333727;
	}

	private void ultimeter(AprsAX25 ax25, String info) {

		int	wind_peak = UNKNOWN;
		int	wind_dir = UNKNOWN;
		int	outdoor_temp = UNKNOWN;
		int	total_rain = UNKNOWN;
		int	barometer = UNKNOWN;
		int	barometer_delta = UNKNOWN;
		int	barometer_correction_l;
		int	barometer_correction_m;
		int	outdoor_humidity = UNKNOWN;
		int	date;
		int	time;
		int	rain_today;
		int	wind_average;

		switch (info.charAt(0)) {
		case '$':

			hex_read = 0;
			try {
				StringReader r = new StringReader(info);

				skip(r, 5);

				wind_peak = hex_short(r);
				wind_dir = hex_short(r);
				outdoor_temp = hex_short(r);
				total_rain = hex_short(r);
				barometer = hex_short(r);
				barometer_delta = hex_short(r);
				barometer_correction_l = hex_short(r);
				barometer_correction_m = hex_short(r);
				outdoor_humidity = hex_short(r);
				date = hex_short(r);
				time = hex_short(r);
				rain_today = hex_short(r);
				wind_average = hex_short(r);
			} catch (IOException ie) {
			}
			if (hex_read >= 11 && hex_read <= 13) {
				message_type = "Ultimeter";
				StringWriter	sw = new StringWriter();
				PrintWriter	pw = new PrintWriter(sw);
				boolean		first = true;

				if (wind_peak != UNKNOWN) {
					pw.format("wind %.1f mph", km_to_miles(wind_peak * 0.1));
					first = false;
				}
				if (wind_dir != UNKNOWN) {
					if (!first)
						pw.format(", ");
					pw.format("direction %.0f", (wind_dir & 0xff) * 360.0 / 256.0);
					first = false;
				}
				if (outdoor_temp != UNKNOWN) {
					if (!first)
						pw.format(", ");
					pw.format("temperature %.1f", outdoor_temp * 0.1);
				}
				if (barometer != UNKNOWN) {
					if (!first)
						pw.format(", ");
					pw.format("barometer %.2f", mbar_to_inhg(barometer * 0.1));
				}
				if (outdoor_humidity != UNKNOWN) {
					if (!first)
						pw.format(", ");
					pw.format("humidity %.0f", outdoor_humidity * 0.1);
				}
				weather = sw.toString();
				comment = null;
			} else {
				System.out.printf("Ultimeter failed %s\n", info);
			}
			break;
		case '!':
			hex_read = 0;
			try {
				StringReader r = new StringReader(info);

				skip(r, 2);
				wind_peak = hex_short(r);
				wind_dir = hex_short(r);
				outdoor_temp = hex_short(r);
				total_rain = hex_short(r);
			} catch (IOException ie) {
			}
			if (hex_read == 4) {
				message_type = "Ultimeter";

				weather = String.format("wind %.1f mph, direction $.0f, temperature %.1f",
							km_to_miles(wind_peak * 0.1),
							(wind_dir & 0xff) * 360.0 / 256.0,
							outdoor_temp * 0.1);
			} else {
				System.out.printf("Ultimeter failed %s\n", info);
			}
			break;
		default:
		}
	}

	private double nmea_pos(String whole, String part, String flag, String plus, String minus) {
		if (whole == null)
			return UNKNOWN;

		int	w = Integer.parseInt(whole);
		int	p = part == null ? 0 : Integer.parseInt(part);
		int	deg = w / 100;
		int	min = w % 100;
		int	sec = p;

		double	v = deg + min / 60.0 + sec / 3600.0;
		if (minus.equalsIgnoreCase(flag))
		    v = -v;
		return v;
	}

	static final String ints = "(\\d+)([.](\\d*))?";
	static final String int_ = "(\\d+)?";
	static final String doub = "(\\d+([.]\\d*)?)?";
	static final String cksum = "\\*(\\d+)";

	static final Pattern gprmc = Pattern.compile("(?s)\\$GPRMC," +
						     ints + ",(.)," +			/* time and status */
						     ints + ",(.)," +			/* lat */
						     ints + ",(.)," +			/* lon */
						     doub + "," +			/* speed */
						     doub + "," +			/* course */
						     ".*");

	private void gprmc(String info) {
		Matcher m = gprmc.matcher(info);
		if (m.matches()) {
			int	g = 1;
			String	time_whole = m.group(g++);
			g++;
			String	time_part = m.group(g++);
			String	status = m.group(g++);
			String	lat_whole = m.group(g++);
			g++;
			String	lat_part = m.group(g++);
			String	lat_flag = m.group(g++);
			String	lon_whole = m.group(g++);
			g++;
			String	lon_part = m.group(g++);
			String	lon_flag = m.group(g++);

			String	speed = m.group(g++);
			g++;

			String	course = m.group(g++);

			latitude = nmea_pos(lat_whole, lat_part, lat_flag, "n", "s");
			longitude = nmea_pos(lon_whole, lon_part, lon_flag, "e", "w");
			if (speed != null)
				speed_mph = knots_to_mph(Double.parseDouble(speed));
			if (course != null)
				this.course = Double.parseDouble(course);
			comment = null;
		} else {
			System.out.printf("\n\tGPRMC failed %s\n\n", info);
		}
	}

	static final Pattern gpgga = Pattern.compile("(?s)\\$GPGGA," +
						     ints + "," +			/* time */
						     ints + ",(.)," +			/* lat */
						     ints + ",(.)," +			/* lon */
						     int_ + "," +			/* quality */
						     int_ + "," +			/* num sat */
						     doub + "," +			/* hdop */
						     doub + "," +			/* alt (m) */
						     ".*");


	private void gpgga(String info) {
		Matcher m = gpgga.matcher(info);
		if (m.matches()) {
			int g = 1;
			String	time_whole = m.group(g++);
			g++;
			String	time_part = m.group(g++);

			String	lat_whole = m.group(g++);
			g++;
			String	lat_part = m.group(g++);
			String	lat_flag = m.group(g++);
			String	lon_whole = m.group(g++);
			g++;
			String	lon_part = m.group(g++);
			String	lon_flag = m.group(g++);

			String	quality = m.group(g++);
			String	nsat = m.group(g++);

			String	hdop = m.group(g++);
			String	alt = m.group(g++);

			latitude = nmea_pos(lat_whole, lat_part, lat_flag, "n", "s");
			longitude = nmea_pos(lon_whole, lon_part, lon_flag, "e", "w");

			if (alt != null)
				altitude_ft = m_to_ft(Double.parseDouble(alt));
			comment = null;
		} else {
			System.out.printf("\n\tGPGGA failed %s\n\n", info);
		}
	}

	private void raw_nmea(AprsAX25 ax25, String info) {
		message_type = info.split("[\\$,]")[1];
		if (info.startsWith("$GPRMC"))
			gprmc(info);
		else if (info.startsWith("$GPGGA"))
			gpgga(info);
		else
			System.out.printf("unknown nmea %s\n", message_type);
	}

	private void set_position(position p) {
		latitude = p.latitude();
		symbol_table = p.sym_table_id;
		longitude = p.longitude();
		symbol_code = p.symbol_code;
		comment = p.comment;
	}

	private void decode_position(String info) {
		position p = new position(info);
		set_position (p);
	}

	private void decode_position_time(String info) {
		position_time p = new position_time(info);
		set_position(p);
	}

	private void set_compressed_position(compressed_position cp) {
		latitude = cp.latitude();
		longitude = cp.longitude();

		symbol_table = cp.symbol_table();
		symbol_code = cp.symbol_code;
		comment = cp.comment;

		if (cp.c == ' ') {
			;	/* ignore other two bytes */
		} else {
			if (((cp.t - 33) & 0x18) == 0x10)
				altitude_ft = Math.pow(1.002, (cp.c - 33) * 91 + cp.s - 33);
			else if (cp.c == '{')
				range = 2.0 * Math.pow(1.08, cp.s - 33);
			else if (cp.c >= '!' && cp.c <= 'z') {
				course = (cp.c - 33) * 4;
				speed_mph = knots_to_mph(Math.pow(1.08, cp.s - 33) - 1.0);
			}
		}
	}

	private void decode_compressed_position(String info) {
		compressed_position cp = new compressed_position(info);
		set_compressed_position(cp);
	}

	private void decode_compressed_position_time(String info) {
		compressed_position_time cp = new compressed_position_time(info);
		set_compressed_position(cp);
	}

	/* Parse weather data if we cared about that */
	private void weather_data(boolean wind_prefix) {
	}

	/*
	 * Pull additional data out of APRS comment field, leaving
	 * anything remaining in the comment field
	 */

	static final Pattern course_speed_comment = Pattern.compile("(?s)^(\\d{3})/(\\d{3})(.*)");
	static final Pattern altitude_comment = Pattern.compile("(?s)(.*)/A=(\\d{6})(.*)");

	private void data_extension_comment() {
		Matcher m;

		if (comment == null)
			return;

		/* CSE/SPD */
		m = course_speed_comment.matcher(comment);

		if (m.matches()) {
			course = Integer.parseInt(m.group(1));
			speed_mph = Integer.parseInt(m.group(2));
			comment = m.group(3);
		}

		/* /A=nnnnnn */
		m = altitude_comment.matcher(comment);

		if (m.matches()) {
			altitude_ft = Integer.parseInt(m.group(2));
			comment = m.group(1) + m.group(3);
		}
	}

	/*
	 * Regular lat/lon position data packet
	 */
	private void ll_pos(AprsAX25 ax25, String info) {
		message_type = "Position";

		if (Character.isDigit(info.charAt(1))) {
			decode_position(info);
			if (symbol_code == '_') {
				message_type = "Weather Report";
				weather_data(true);
			} else {
				data_extension_comment();
			}
		} else {
			decode_compressed_position(info);
			data_extension_comment();
		}
	}

	/*
	 * Lat/lon position with time
	 */
	private void ll_pos_time(AprsAX25 ax25, String info) {
		message_type = "Position with time";
		if (Character.isDigit(info.charAt(1))) {
			decode_position_time(info);
			if (symbol_code == '_') {
				message_type = "Weather Report";
				weather_data(true);
			} else {
				data_extension_comment();
			}
		} else {
			decode_compressed_position_time(info);
			data_extension_comment();
		}
	}

	/*
	 * The crazy mic_e format, which overloads the destination
	 * callsign with position information. Its very short at least?
	 */
	private void mic_e(AprsAX25 ax25, String info) {
		message_type = "MIC_E";

		mic_e m = new mic_e(info);
		String dest = ax25.address(AprsAX25.AX25_DESTINATION).callsign;

		latitude = (m.digit(dest.charAt(0), 4) * 10 +
			    m.digit(dest.charAt(1), 2) +
			    (m.digit(dest.charAt(2), 1) * 1000 +
			     m.digit(dest.charAt(3), 0) * 100 +
			     m.digit(dest.charAt(4), 0) * 10 +
			     m.digit(dest.charAt(5), 0)) / 6000.0);

		int dest3 = dest.charAt(3);
		if ('0' <= dest3 && dest3 <= '9' || dest3 == 'L')
			latitude = -latitude;

		int offset = 0;
		int dest4 = dest.charAt(4);
		if ('0' <= dest4 && dest4 <= '9' || dest4 == 'L')
			offset = 0;
		else if ('P' <= dest4 && dest4 <= 'Z')
			offset = 1;

		int lon0 = m.lon.charAt(0);
		if (offset > 0 && 118 <= lon0 && lon0 <= 127)
			longitude = lon0 - 118;			/* 0-9 degrees */
		else if (offset == 0 && 38 <= lon0 && lon0 <= 127)
			longitude = (lon0 - 38) + 10;		/* 10 - 99 degrees */
		else if (offset > 0 && 108 <= lon0 && lon0 <= 117)
			longitude = (lon0 - 108) + 100;		/* 100 - 109 degrees */
		else if (offset > 0 && 38 <= lon0 && lon0 <= 107)
			longitude = (lon0 - 38) + 110;		/* 110 - 179 degrees */

		if (longitude != UNKNOWN) {
			int lon1 = m.lon.charAt(1);
			if (lon1 >= 88 && lon1 <= 97)
				longitude += (lon1 - 88) / 60.0;		/* 0 - 9 minutes*/
			else if (lon1 >= 38 && lon1 <= 87)
				longitude += ((lon1 - 38) + 10) / 60.0;	/* 10 - 59 minutes */
		}
		if (longitude != UNKNOWN) {
			int lon2 = m.lon.charAt(2);

			if (lon2 >= 28 && lon2 <= 127) 
				longitude += ((lon2 - 28) + 0) / 6000.0;	/* 0 - 99 hundredths of minutes*/
		}
		if (longitude != UNKNOWN) {
			int dest5 = dest.charAt(5);
			if ('P' <= dest5 && dest5 <= 'Z')
				longitude = -longitude;
		}

		int sp = m.speed_course.charAt(0) - 28;
		int dc = m.speed_course.charAt(1) - 28;
		int se = m.speed_course.charAt(2) - 28;

		speed_mph = sp * 10 + (dc / 10) % 10;
		if (speed_mph >= 800)
			speed_mph -= 800;

		course = (dc % 10) * 100 + se;

		if (course >= 400)
			course -= 400;

		symbol_code = m.symbol_code;
		symbol_table = m.sym_table_id;

		/* ignore the status field */
		comment = null;
	}

	/*
	 * Parse a pile of other messages. Meh
	 */
	private void station_cap(AprsAX25 ax25, String info) {
		message_type = "Station Capabilities";
		comment = info.substring(1);
	}

	private void status(AprsAX25 ax25, String info) {
		message_type = "Status Report";
		comment = info.substring(1);
	}

	private void weather(AprsAX25 ax25, String info) {
		message_type = "Weather";
		comment = info.substring(1);
	}

	private void third_party(AprsAX25 ax25, String info) {
		message_type = "Third Party";
		comment = info.substring(1);
	}

	private void message(AprsAX25 ax25, String info) {
		message_type = "Message";

		message m = new message(info);
		addressee = m.addressee.trim();
		message = m.message;
		comment = null;
	}

	private void object(AprsAX25 ax25, String info) {

		message_type = "Object";

		if (Character.isDigit(info.charAt(18))) {
			object o = new object(info);
			name = o.name.trim();
			latitude = o.latitude();
			longitude = o.longitude();
			symbol_code = o.symbol_code;
			symbol_table = o.sym_table_id;
			comment = o.comment;
		} else {
			compressed_object o = new compressed_object(info);
			name = o.name.trim();
			set_compressed_position(o);
			latitude = o.latitude();
		}
		data_extension_comment();
	}

	private void clean_comment() {
		StringBuilder s = new StringBuilder();

		if (comment == null)
			return;

		if (comment.equals("")) {
			comment = null;
			return;
		}

		for (int i = 0; i < comment.length(); i++) {
			char c = comment.charAt(i);

			if (' ' <= c && c <= '~')
				s.append(c);
		}
		comment = s.toString();
	}

	/*
	 * Construct a string representation of the APRS data.
	 * This is mostly for debugging
	 */
	public String toString() {
		StringWriter	sw = new StringWriter();
		PrintWriter	pw = new PrintWriter(sw);

		if (source != null)
			pw.format("%s ", source.toString());
		pw.format("%c", symbol_table);
		if (symbol_code != ' ')
			pw.format(":%c", symbol_code);
		pw.format(" %s", message_type);
		if (name != null)
			pw.format(" %s", name);
		if (latitude != UNKNOWN)
			pw.format(" lat %f", latitude);
		if (longitude != UNKNOWN)
			pw.format(" lon %f", longitude);
		if (speed_mph != UNKNOWN)
			pw.format(" speed %f", speed_mph);
		if (course != UNKNOWN)
			pw.format(" course %f", course);
		if (altitude_ft != UNKNOWN)
			pw.format(" alt %f", altitude_ft);
		if (weather != null)
			pw.format(" weather %s", weather);
		if (comment != null)
			pw.format(" %s", comment);

		if (addressee != null)
			pw.format(" to: %s", addressee);
		if (message != null)
			pw.format(" msg: %s", message);
		return sw.toString();
	}

	public AprsAprs(AprsAX25 ax25) {
		String info = ax25.info();
		char dti = '?';

		symbol_table = '/';
		symbol_code = ' ';

		comment = info;
		if (info != null && info.length() > 0) {
			dti = info.charAt(0);
			comment = info.substring(1);
		}

		try {

			source = ax25.address(AprsAX25.AX25_SOURCE);
			destination = ax25.address(AprsAX25.AX25_DESTINATION);

			/* Handle a useful subset of the available message types */
			switch (dti) {
			case type_position:
			case type_position_msg:
				if (info.startsWith("!!"))
					ultimeter(ax25, info);
				else
					ll_pos(ax25, info);
				break;

			case type_raw_gps:
				if (info.startsWith("$ULTW"))
					ultimeter(ax25, info);
				else
					raw_nmea(ax25, info);
				break;

			case type_mic_e:
			case type_old_mic_e_rev_0:
			case type_old_mic_e:
			case type_current_mic_e:
				mic_e(ax25, info);
				break;

			case type_position_time:
			case type_position_time_msg:
				ll_pos_time(ax25, info);
				break;

			case type_station_cap:
				station_cap(ax25, info);
				break;

			case type_status:
				status(ax25, info);
				break;

			case type_weather:
				weather(ax25, info);
				break;

			case type_third_party:
				third_party(ax25, info);
				break;

			case type_message:
				message(ax25, info);
				break;

			case type_object:
				object(ax25, info);
				break;
			default:
				message_type = String.format("Unknown message type '%c'", dti);
				break;
			}

		} catch (Exception e) {
			message_type = String.format("Invalid message of type '%c'", dti);
		}
		clean_comment();
	}
}
