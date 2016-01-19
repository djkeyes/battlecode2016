package dk010;

import battlecode.common.MapLocation;

public class RushHandler extends BaseHandler {

	public static MapLocation targetLoc = null;
	public static MapLocation furthestFromGatheringLoc = null;

	public static void determineRushTargets() {
		// to determine which enemy to track, first we figure out which archon
		// built us
		// then we travel to the corresponding archon index
		// (if our archons are too close to tell accurately, then it probably
		// doesn't matter if we get it wrong.)

		int minDistSq = Integer.MAX_VALUE;
		int closestArchonIdx = 0;
		MapLocation[] archonLocs = rc.getInitialArchonLocations(us);
		MapLocation[] enemyArchonLocs = rc.getInitialArchonLocations(them);
		MapLocation curLoc = rc.getLocation();
		for (int i = 0; i < archonLocs.length; i++) {
			int distSq = curLoc.distanceSquaredTo(archonLocs[i]);
			if (distSq < minDistSq) {
				minDistSq = distSq;
				closestArchonIdx = i;
			}
		}

		// also find the farthest archon, as a backup
		int maxDistSq = -1;
		for (int i = 0; i < enemyArchonLocs.length; i++) {
			int distSq = curLoc.distanceSquaredTo(enemyArchonLocs[i]);
			if (distSq > maxDistSq) {
				minDistSq = distSq;
				furthestFromGatheringLoc = enemyArchonLocs[i];
			}
		}

		targetLoc = enemyArchonLocs[closestArchonIdx];
	}

}
