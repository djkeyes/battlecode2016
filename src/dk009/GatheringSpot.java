package dk009;

import battlecode.common.MapLocation;
import battlecode.common.RobotType;

public class GatheringSpot extends BaseHandler {

	public static MapLocation gatheringSpot = null;

	public static void init() {
		gatheringSpot = rc.getInitialArchonLocations(us)[0];
	}

	public static void updateGatheringSpot(SignalContents[] decodedSignals) {
		MapLocation closestDen = null;
		int minDenDistSq = Integer.MAX_VALUE;
		for (SignalContents s : decodedSignals) {
			if (s.type == RobotType.ZOMBIEDEN) {
				MapLocation cur = new MapLocation(s.x, s.y);
				int dist = gatheringSpot.distanceSquaredTo(cur);
				if (dist < minDenDistSq) {
					minDenDistSq = dist;
					closestDen = cur;
				}

			}
		}

		if (closestDen != null) {
			gatheringSpot = closestDen;
		}
	}
}
