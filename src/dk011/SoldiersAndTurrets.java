package dk011;

import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

public class SoldiersAndTurrets implements Strategy {

	@Override
	public RobotType getNextToBuild(RobotInfo[] curAlliesInSight) {
		// TODO: build a soldier in certain situations, if we need fast defense

		boolean shouldBuildScout = true;
		for (RobotInfo ally : curAlliesInSight) {
			if (ally.type == RobotType.SCOUT) {
				shouldBuildScout = false;
				break;
			}
		}

		if (shouldBuildScout) {
			return RobotType.SCOUT;
		} else {
			return RobotType.SOLDIER;
		}
	}

	@Override
	public void incrementNextToBuild() {
	}

}
