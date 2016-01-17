package dk009;

import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

public class SoldierMass implements Strategy {

	@Override
	public RobotType getNextToBuild(RobotInfo[] nearbyAllies) {
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
