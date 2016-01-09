package dk003;

import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;

public class Soldier extends BaseHandler {

	public static void run() throws GameActionException {
		// boolean goingNorth = true;
		while (true) {
			beginningOfLoop();
			Mapping.updateMap();

			MapLocation goal = new MapLocation(Mapping.startCol - 22, Mapping.startRow + 15);
			if(!AStarPathing.aStarToward(goal)){
				if(rc.isCoreReady() && rc.isWeaponReady()){
					rc.attackLocation(goal);
				}
			}
			Clock.yield();
		}
	}
}
