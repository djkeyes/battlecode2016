package dk011;

import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

public class SoldiersAndTurrets extends BaseHandler implements Strategy {

	public static final int POP_TO_MASS_TURRETS = 30;

	private RobotType lastUnitType = null;
	private boolean builtTurretLast = false;

	@Override
	public RobotType getNextToBuild(RobotInfo[] curAlliesInSight) {
		// TODO: build a soldier in certain situations, if we need fast defense

		int minSoldierCount = 2;
		if (rc.getRobotCount() > 20) {
			minSoldierCount = 1;
		}
		boolean shouldBuildSoldier = true;
		int numSoldiersNearby = 0;
		for (RobotInfo ally : curAlliesInSight) {
			if (ally.type == RobotType.SOLDIER) {
				numSoldiersNearby++;
				if (numSoldiersNearby >= minSoldierCount) {
					shouldBuildSoldier = false;
					break;
				}
			}
		}
		if (shouldBuildSoldier) {
			return lastUnitType = RobotType.SOLDIER;
		}

		if (rc.getRobotCount() >= POP_TO_MASS_TURRETS) {
			if (builtTurretLast) {
				return lastUnitType = RobotType.SCOUT;
			} else {
				return lastUnitType = RobotType.TURRET;
			}
		}

		boolean shouldBuildScout = true;
		for (RobotInfo ally : curAlliesInSight) {
			if (ally.type == RobotType.SCOUT) {
				shouldBuildScout = false;
				break;
			}
		}

		if (shouldBuildScout) {
			return lastUnitType = RobotType.SCOUT;
		} else {
			return lastUnitType = RobotType.SOLDIER;
		}
	}

	@Override
	public void incrementNextToBuild() {
		builtTurretLast = lastUnitType == RobotType.TURRET;
	}

}
