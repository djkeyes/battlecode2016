package dk011;

import battlecode.common.Clock;
import battlecode.common.Signal;

public class BasicAttacker extends BaseHandler {

	public static void run() {

		while (true) {
			beginningOfLoop();

			loop();

			Clock.yield();

		}
	}

	public static void loop() {

		String foo = "";
		Signal[] signals = rc.emptySignalQueue();
		for (Signal s : signals) {
			foo = foo + ", " + s.getMessage()[0];
		}

		rc.setIndicatorString(0, foo);
	}
}
