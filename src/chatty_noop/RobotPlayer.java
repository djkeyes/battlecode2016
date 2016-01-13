package chatty_noop;

import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.RobotController;

// a player that does nothing, for testing.
// this one makes one broadcast at the beginning, which may be common among other teams
public class RobotPlayer {

	public static void run(RobotController rc) {
		try {
			rc.broadcastSignal(GameConstants.MAP_MAX_HEIGHT * GameConstants.MAP_MAX_HEIGHT
					+ GameConstants.MAP_MAX_WIDTH * GameConstants.MAP_MAX_WIDTH);
		} catch (GameActionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		while (true) {
			Clock.yield();
		}
	}
}
