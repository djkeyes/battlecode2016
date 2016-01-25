package team292;

import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

public class ArchonReporter extends ArchonReceiver {

	public static void reportArchonLocation(MapLocation loc, int robotId, int broadcastRadiusSq)
			throws GameActionException {
		Messaging.sendMessage(new ArchonLocationMessage(loc, robotId), broadcastRadiusSq);
	}

	public static void reportArchonLocations(RobotInfo[] nearbyAllies, int broadcastRadiusSq)
			throws GameActionException {
		for (RobotInfo ally : nearbyAllies) {
			if (rc.getMessageSignalCount() >= GameConstants.MESSAGE_SIGNALS_PER_TURN) {
				break;
			}
			if (ally.type == RobotType.ARCHON) {
				reportArchonLocation(ally.location, ally.ID, broadcastRadiusSq);
			}
		}
	}

	public static class ArchonLocationMessage extends Message {

		private MapLocation loc;
		private int robotId;

		public ArchonLocationMessage(MapLocation loc, int robotId) {
			this.loc = loc;
			this.robotId = robotId;
		}

		@Override
		protected long getMessageOrdinal() {
			return ARCHON_LOCATION_MESSAGE;
		}

		@Override
		public int[] encodeMessage() {
			appendData(robotId, Integer.MAX_VALUE);
			appendData(loc.x, 581);
			appendData(loc.y, 581);
			return super.encodeMessage();
		}

		public static void processMessage(long allBits) {
			setBits(allBits);

			int y = (int) consumeData(581);
			int x = (int) consumeData(581);
			int robotId = (int) consumeData(Integer.MAX_VALUE);

			MapLocation loc = new MapLocation(x, y);

			ArchonReceiver.recordArchonPosition(loc, robotId);
		}
	}
}
