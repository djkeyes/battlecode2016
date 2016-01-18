package dk009;

import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

public class SoldierMass extends BaseHandler implements Strategy {

	@Override
	public RobotType getNextToBuild(RobotInfo[] nearbyAllies) {
		// don't bother building scouts if they wouldn't have time to see anything
		if (WaxAndWane.zombiesAreNigh(curTurn + RobotType.SCOUT.buildTurns, 31)) {
			return RobotType.SOLDIER;
		}

		for (RobotInfo ally : nearbyAllies) {
			if (ally.type == RobotType.SCOUT) {
				return RobotType.SOLDIER;
			}
		}
		return RobotType.SCOUT;
	}

	@Override
	public void incrementNextToBuild() {
	}

}
