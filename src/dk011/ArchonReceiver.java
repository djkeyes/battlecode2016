package dk011;

import battlecode.common.MapLocation;

public class ArchonReceiver extends BaseHandler {

	// a map can start with 4 archons and have 4 neutrals
	public static final int MAX_NUM_ARCHONS = 8;

	public static final MapLocation[] archonLocs = new MapLocation[MAX_NUM_ARCHONS];
	public static final int[] archonIds = new int[MAX_NUM_ARCHONS];

}
