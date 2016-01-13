package dk006;

import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;

// reworking some stuff
// goals for this package:
// -build soldiers, attack at some point
// -do intelligent micro
// -construct a local world map
// -do intelligent pathfinding
// I saw some other teams doing abstract static classes. This is dumb, since I
// think that incurs higher bytecode costs--but it's easier to read, since
// static variables in a parent are accessible in child classes.
public class RobotPlayer {

	public static void run(RobotController rc) {
		BaseHandler.init(rc);
		// loop around try/catch so that we can restart if something breaks.
		// actual bots should run their own game loop.
		while (true) {
			try {
				BaseHandler.run();
			} catch (GameActionException ex) {
				// so called "debug_" methods no longer exist
				// so make sure to comment this out for final submissions.
				ex.printStackTrace();
				// end the current turn before trying to restart
				Clock.yield();
			}
		}
	}
}
