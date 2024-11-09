package aprslib_1;

import org.altusmetrum.aprslib_1.AprsTest;

public class Launcher {

	public static void main(final String[] args) {
		AprsTest t = new AprsTest();

		// String[] args2 = new String[] {
		// "src/main/resources/keith-packard/tnc_test01a.raw" };
		// String[] args2 = new String[] { "src/main/resources/direwolf/test1.wav" };
		// String[] args2 = new String[] {
		// "src/main/resources/wmarkow/144800MHz_recording.wav" };
		String[] args2 = new String[] { "src/main/resources/wmarkow/HC12_fox_example.wav" };

		t.run(args2);
	}
}
