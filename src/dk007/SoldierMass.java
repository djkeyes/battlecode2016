package dk007;

import battlecode.common.RobotType;

public class SoldierMass implements Strategy {

	@Override
	public RobotType getNextToBuild() {
		return RobotType.SOLDIER;
	}

	@Override
	public void incrementNextToBuild() {
	}

}
