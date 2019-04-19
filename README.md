# Aprslib — Java library for APRS decoding

Aprslib is a suite of Java code designed to decode amateur radio
position reporting system (APRS) packets from a stream of digital
audio samples.

There are lots of knobs to play with, including a number of filter
selection options and a choice between sin/cos convolution detectors
and Goertzel filter detectors. The former are a bit more accurate, the
latter requires a lot less computation.

## Build

	$ ./autogen.sh
	$ make

## Test

	$ ./aprstest tnc_test01a.raw tnc_test01b.raw
