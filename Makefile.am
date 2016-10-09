AM_JAVACFLAGS=-target 1.6 -encoding UTF-8 -Xlint:deprecation -Xlint:unchecked -source 6
JAVAROOT=bin

VERSION=1

APRSLIB_VERSION=1

CLASSPATH_ENV=mkdir -p $(JAVAROOT); CLASSPATH="bin:/usr/share/java/*"

SRC=.

aprslibdir = $(datadir)/java

aprslib_JAVA = \
	AprsAddress.java \
	AprsAprs.java \
	AprsAX25.java \
	AprsAgc.java \
	AprsBit.java \
	AprsData.java \
	AprsDemod.java \
	AprsFcs.java \
	AprsHdlc.java \
	AprsFilter.java \
	AprsPacket.java \
	AprsPll.java \
	AprsRing.java \
	AprsTest.java

JAR=aprslib_$(APRSLIB_VERSION).jar

all-local: $(JAR) aprstest

clean-local:
	-rm -rf $(JAVAROOT) aprslib_*.jar

install-aprslibJAVA: $(JAR)
	@$(NORMAL_INSTALL)
	test -z "$(aprslibdir)" || $(MKDIR_P) "$(DESTDIR)$(aprslibdir)"
	echo " $(INSTALL_DATA)" "$<" "'$(DESTDIR)$(aprslibdir)/$(JAR)"; \
	$(INSTALL_DATA) "$<" "$(DESTDIR)$(aprslibdir)"

bin:
	mkdir -p bin

$(JAR): classaprslib.stamp Manifest.txt
	jar cfm $@ Manifest.txt -C bin org

Manifest.txt: Makefile
	echo 'Main-Class: org.altusmetrum.aprslib_$(APRSLIB_VERSION).AprsTest' > $@

aprstest: Makefile
	echo '#!/bin/sh' > $@
	echo 'exec java -jar $(JAR) "$$@"' >> $@
	chmod +x $@
