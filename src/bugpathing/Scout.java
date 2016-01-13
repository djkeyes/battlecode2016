package bugpathing;

import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;

public class Scout extends BaseHandler {

	public static void run() throws GameActionException {

		while (true) {
			beginningOfLoop();

			Pathfinding.setTarget(new MapLocation(421, 172), true);
			if (rc.isCoreReady()) {
				Pathfinding.pathfindToward();
			}
			
			Clock.yield();
		}
	}
}
