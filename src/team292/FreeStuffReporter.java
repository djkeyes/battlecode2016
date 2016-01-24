package team292;

import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

public class FreeStuffReporter extends FreeStuffReceiver {

	public static void reportFreeStuff(RobotInfo[] nearbyNeutrals, int neutralBroadcastRadiusSq,
			MapLocation[] nearbyParts, int partsBroadcastRadiusSq, boolean okayToShoutArchons)
			throws GameActionException {
		int allMapRadius = 0;
		if (okayToShoutArchons) {
			allMapRadius = MapEdgesReceiver.getMinAllMapRadius();
		}
		// run the neutrals first, since they give a more immediate advantange
		for (int i = 0; i < nearbyNeutrals.length; i++) {
			if (rc.getMessageSignalCount() == GameConstants.MESSAGE_SIGNALS_PER_TURN) {
				// :(
				break;
			}
			RobotInfo cur = nearbyNeutrals[i];

			if (!tryAddNeutralUnit(cur.location)) {
				continue;
			}

			Message m = new FreeStuffMessage(true, cur.location);

			if (!okayToShoutArchons || cur.type != RobotType.ARCHON) {
				Messaging.sendMessage(m, neutralBroadcastRadiusSq);
			} else {
				Messaging.sendMessage(m, allMapRadius);
			}

		}

		for (int i = 0; i < nearbyParts.length; i++) {
			if (rc.getMessageSignalCount() == GameConstants.MESSAGE_SIGNALS_PER_TURN) {
				// :(
				break;
			}
			MapLocation cur = nearbyParts[i];
			double rubble = rc.senseRubble(cur);
			if (rubble >= GameConstants.RUBBLE_OBSTRUCTION_THRESH) {
				continue;
			}

			if (!tryAddParts(cur)) {
				continue;
			}

			Message m = new FreeStuffMessage(false, cur);
			Messaging.sendMessage(m, partsBroadcastRadiusSq);
		}
	}

	public static void maybeAnnounceNeutralActivated(MapLocation neutralLoc) {
		// if there are several neutrals clustered together, it could be
		// ambiguous which one we moved. however the activation radius is very
		// small, so even if we remove the wrong one, we should still be in the
		// ballpark.

		// TODO: announce the location
	}

	public static void announcePartsEaten() {
		// TODO: announce that we just ate whatever was at curLoc
	}
}
