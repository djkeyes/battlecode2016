package dk011;

import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

public class SoldiersAndTurrets extends BaseHandler implements Strategy {

	public final int POP_TO_MASS_TURRETS;

	public SoldiersAndTurrets() {
		POP_TO_MASS_TURRETS = rc.getInitialArchonLocations(us).length * 13;
	}

	private RobotType lastUnitType = null;
	private boolean builtTurretLast = false;

	private static final int NUM_TURRETS_PER_VIPER = 6;
	private int numTurretsSinceLastViper = 6;

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

		if (isMassingTurrets()) {
			if (builtTurretLast) {
				return lastUnitType = RobotType.SCOUT;
			} else {
				if (numTurretsSinceLastViper >= NUM_TURRETS_PER_VIPER) {
					return lastUnitType = RobotType.VIPER;
				} else {
					return lastUnitType = RobotType.TURRET;
				}
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

	public boolean isMassingTurrets() {
		return rc.getRobotCount() >= POP_TO_MASS_TURRETS;
	}

	@Override
	public void incrementNextToBuild() {
		builtTurretLast = lastUnitType == RobotType.TURRET;

		if (builtTurretLast) {
			numTurretsSinceLastViper++;
		} else if (lastUnitType == RobotType.VIPER) {
			numTurretsSinceLastViper = 0;
		}
	}

}
