dnl
dnl  Copyright © 2019 Keith Packard <keithp@keithp.com>
dnl
dnl  This program is free software; you can redistribute it and/or modify
dnl  it under the terms of the GNU General Public License as published by
dnl  the Free Software Foundation; either version 2 of the License, or
dnl  (at your option) any later version.
dnl
dnl  This program is distributed in the hope that it will be useful, but
dnl  WITHOUT ANY WARRANTY; without even the implied warranty of
dnl  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
dnl  General Public License for more details.
dnl
dnl  You should have received a copy of the GNU General Public License along
dnl  with this program; if not, write to the Free Software Foundation, Inc.,
dnl  59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
dnl
dnl Process this file with autoconf to create configure.

AC_PREREQ(2.57)
AC_INIT([aprslib], 0.9)
AC_CONFIG_SRCDIR([AprsAprs.java])
AM_INIT_AUTOMAKE([foreign dist-bzip2])

VERSION_DASH=`echo $VERSION | sed 's/\./-/g'`
AC_SUBST(VERSION_DASH)

dnl ==========================================================================
dnl Java library versions

APRSLIB_VERSION=1

AC_SUBST(APRSLIB_VERSION)
AC_DEFINE(APRSLIB_VERSION,$APRSLIB_VERSION,[Version of the AprsLib package])

dnl ==========================================================================

AC_ARG_WITH(jvm, AS_HELP_STRING([--with-jvm-include=PATH],
	[Set jvm include path for jni builds (default searches in /usr/lib/jvm)]),
	[JVM_INCLUDE=$withval], [JVM_INCLUDE=auto])

if test "x$JVM_INCLUDE" = "xauto"; then
	AC_MSG_CHECKING([JVM include files])
	for jvm in default-java java-6-openjdk java-6-sun; do
		if test "x$JVM_INCLUDE" = "xauto"; then
			INCLUDE="/usr/lib/jvm/$jvm/include"
			if test -f "$INCLUDE"/jni.h; then
				JVM_INCLUDE="$INCLUDE"
			fi
		fi
	done
	if test "x$JVM_INCLUDE" = "xauto"; then
		AC_MSG_ERROR([no JVM include files found])
	fi
	AC_MSG_RESULT([$JVM_INCLUDE])
fi

AC_SUBST(JVM_INCLUDE)

AC_OUTPUT([
Makefile
])

echo ""
echo "  Package: ${PACKAGE_NAME} ${PACKAGE_VERSION}"
echo ""
echo "  Java"
echo "    JVM include.................: ${JVM_INCLUDE}"
echo ""
