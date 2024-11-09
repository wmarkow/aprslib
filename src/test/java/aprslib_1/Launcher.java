package aprslib_1;

import org.altusmetrum.aprslib_1.AprsTest;

public class Launcher {
	
	public static void main(final String[] args) {
		AprsTest t = new AprsTest();

		String[] args2 = new String[] { "src/main/resources/tnc_test01a.raw" };

		t.run(args2);
	}
}
