package dk011;

import battlecode.common.Clock;
import battlecode.common.RobotController;

public class RobotPlayer {

	public static void run(RobotController rc) {
		BaseHandler.init(rc);
		// loop around try/catch so that we can restart if something breaks.
		// actual bots should run their own game loop for each round.
		while (true) {
			try {
				BaseHandler.run();
			} catch (Exception ex) {
				// so called "debug_" methods no longer exist
				// so make sure to comment this out for final submissions.
				ex.printStackTrace();
				// end the current turn before trying to restart
				Clock.yield();
			}
		}
	}
}
