package team292;

import battlecode.common.MapLocation;
import battlecode.common.RobotType;
import battlecode.common.Signal;

// to be honest, this class should probably be declared in EnemyUnitReceiver, since it access so may things in that class
public class FreeStuffMessage extends Message {

	@Override
	protected long getMessageOrdinal() {
		return FREE_STUFF_MESSAGE;
	}

	public static final int MAX_X = 581;
	public static final int MAX_Y = 581;
	private boolean isNeutralUnit;
	private MapLocation loc;

	/******** SENDING *********/

	public FreeStuffMessage(boolean isNeutralUnit, MapLocation loc) {
		this.isNeutralUnit = isNeutralUnit;
		this.loc = loc;
	}

	public int[] encodeMessage() {
		appendData(isNeutralUnit ? 1 : 0, 2);
		appendData(loc.x, MAX_X);
		appendData(loc.y, MAX_Y);

		return super.encodeMessage();
	}

	/******* RECEIVING ********/

	public static void processMessage(long bits) {
		if (BaseHandler.rc.getType() == RobotType.ARCHON) {
			setBits(bits);

			int yLoc = (int) consumeData(MAX_Y);
			int xLoc = (int) consumeData(MAX_X);
			MapLocation loc = new MapLocation(xLoc, yLoc);
			boolean isNeutralUnit = consumeData(2) > 0;

			if (isNeutralUnit) {
				FreeStuffReceiver.tryAddNeutralUnit(loc);
			} else {
				FreeStuffReceiver.tryAddParts(loc);
			}
		}

	}
}
