package dk011;

import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

public class ArchonReceiver extends BaseHandler {

	// a map can start with 4 archons and have 4 neutrals
	public static final int MAX_NUM_ARCHONS = 8;

	public static final MapLocation[] archonLocs = new MapLocation[MAX_NUM_ARCHONS];
	public static final int[] archonIds = new int[MAX_NUM_ARCHONS];

	public static void recordArchonPosition(MapLocation loc, int robotId) {
		int i = 0;
		for (; i < MAX_NUM_ARCHONS && archonIds[i] != robotId; ++i)
			;

		// if our id isn't already in the list, find an entry that's null
		// and use it
		if (i == MAX_NUM_ARCHONS) {
			for (i = 0; archonLocs[i] != null; ++i)
				;
		}

		archonLocs[i] = loc;
		archonIds[i] = robotId;
	}

	public static void updateWithVisibleArchons(RobotInfo[] curAlliesInSight) {
		for (int i = curAlliesInSight.length; --i >= 0;) {
			if (curAlliesInSight[i].type == RobotType.ARCHON) {
				recordArchonPosition(curAlliesInSight[i].location, curAlliesInSight[i].ID);
			}
		}
	}

}
