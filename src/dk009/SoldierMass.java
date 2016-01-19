package dk009;

import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

public class SoldierMass extends BaseHandler implements Strategy {

	private static final int TURRET_POPULATION_THRESHOLD = 30;
	private static final int SOLDIERS_PER_TURRET = 5;
	private static int soldierCounter = 0;

	private boolean isSpawnScheduleHard = false;
	public static int LAST_RUSH_ROUND = 39;

	public SoldierMass() {
		if (zombieSpawnTurns.length > 0) {
			// not sure what a good divider is here. Some maps have spawns on 0,
			// 50, 100, and some maps don't start until 150. we should do some
			// research.
			if (zombieSpawnTurns[0] <= 75) {
				isSpawnScheduleHard = true;
			}
		}
	}

	@Override
	public RobotType getNextToBuild(RobotInfo[] nearbyAllies) {
		if (isRush()) {
			if (isSpawnScheduleHard) {
				// the map already provides the zombies for us
				return RobotType.SCOUT;
			} else {
				return RobotType.VIPER;
			}
		}

		// don't bother building scouts if they wouldn't have time to see
		// anything
		boolean shouldBuildScout = WaxAndWane.zombiesAreNigh(curTurn + RobotType.SCOUT.buildTurns, 51);

		if (shouldBuildScout) {
			for (RobotInfo ally : nearbyAllies) {
				if (ally.type == RobotType.SCOUT) {
					shouldBuildScout = false;
				}
			}
		}

		if (shouldBuildScout) {
			return RobotType.SCOUT;
		} else {
			if (rc.getRobotCount() >= TURRET_POPULATION_THRESHOLD && soldierCounter == 0) {
				return RobotType.TURRET;
			}
			return RobotType.SOLDIER;
		}
	}

	@Override
	public void incrementNextToBuild() {
		soldierCounter = (soldierCounter + 1) % SOLDIERS_PER_TURRET;
	}

	@Override
	public boolean isRush() {
		return curTurn <= LAST_RUSH_ROUND;
	}

}
