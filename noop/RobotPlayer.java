package noop;

import battlecode.common.Clock;
import battlecode.common.RobotController;

// a player that does nothing, for testing.
public class RobotPlayer {

	public static void run(RobotController rc) {
		while (true) {
			Clock.yield();
		}
	}
}
