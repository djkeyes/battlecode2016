package dk003;

import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;

public class Scout extends BaseHandler {

	public static boolean success = false;

	public static void run() throws GameActionException {
		while (true) {
			beginningOfLoop();
			Mapping.updateMap();

			MapLocation goal = new MapLocation(421, 144);
			AStarPathing.aStarToward(goal);

			if (!success && goal.equals(rc.getLocation())) {
				success = true;
				System.out.println("success!");
			}
			Clock.yield();
		}
	}
}
