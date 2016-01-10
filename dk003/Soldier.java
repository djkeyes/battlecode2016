package dk003;

import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;

public class Soldier extends BaseHandler {

	public static void run() throws GameActionException {
		while (true) {
			beginningOfLoop();
			Mapping.updateMap();

			MapLocation goal = new MapLocation(421, 144);
			AStarPathing.aStarToward(goal);
			
			if(goal.equals(rc.getLocation())){
				System.out.println("success!");
			}
			Clock.yield();
		}
	}
}
