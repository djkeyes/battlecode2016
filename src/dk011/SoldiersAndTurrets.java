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
			rc.setIndicatorString(0, "building soldier for defence, last unit: " + lastUnitType
					+ ", and last successful was turret: " + builtTurretLast);
			return lastUnitType = RobotType.SOLDIER;
		}

		if (rc.getRobotCount() >= POP_TO_MASS_TURRETS) {
			if (builtTurretLast) {
				rc.setIndicatorString(0, "building scout to pair with turret, last unit: " + lastUnitType
						+ ", and last successful was turret: " + builtTurretLast);
				return lastUnitType = RobotType.SCOUT;
			} else {
				rc.setIndicatorString(0, "building turret, last unit: " + lastUnitType
						+ ", and last successful was turret: " + builtTurretLast);
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
			rc.setIndicatorString(0, "building scout for scouting, last unit: " + lastUnitType
					+ ", and last successful was turret: " + builtTurretLast);
			return lastUnitType = RobotType.SCOUT;
		} else {
			rc.setIndicatorString(0, "building soldier for massing, last unit: " + lastUnitType
					+ ", and last successful was turret: " + builtTurretLast);
			return lastUnitType = RobotType.SOLDIER;
		}
	}

	@Override
	public void incrementNextToBuild() {
		builtTurretLast = lastUnitType == RobotType.TURRET;
	}

}
