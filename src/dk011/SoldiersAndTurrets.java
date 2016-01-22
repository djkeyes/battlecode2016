package dk011;

import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

public class SoldiersAndTurrets implements Strategy {

	private static boolean builtScout = false;

	@Override
	public RobotType getNextToBuild(RobotInfo[] curAlliesInSight) {
		// TODO: build a soldier in certain situations, if we need fast defense
		if (!builtScout) {
			return RobotType.SCOUT;
		}
		return RobotType.SOLDIER;
	}

	@Override
	public void incrementNextToBuild() {
		builtScout = true;
	}

}
