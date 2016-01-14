package baselinemicro;

import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

public class Micro extends BaseHandler {

	// TODO: none of this takes into account min turret range
	// with lone turrets, your best strategy is to rush them and get under their
	// minimum range
	public static void doMicro(RobotInfo[] nearbyAllies, RobotInfo[] nearbyEnemies) throws GameActionException {
		// TODO: this doesn't take into account broadcasted signals
		// if enemy turrets are broadcasted, that's important for pathing. but
		// if there are duplicate messages, then all our heuristics are fucked.
		// so we need to do some deduping before that's a viable source of
		// information...
		// ...on the other hand, messages are also a good source of information
		// about places to go if robots don't have any targets, and implementing
		// that doesn't really require deduping.

		// first check if anyone can shoot us
		// if the ones close-by are too powerful, retreat
		// then check if we can shoot anyone
		// if we can, shoot them
		// otherwise advance

		// TODO: this assume all units are equal.
		// we should probably weigh these somehow. things with long range
		// are more powerful. things with high dps and low health are also
		// juicy targets.
		int numCanShootUs = 0;
		boolean[] canShootThem = new boolean[nearbyAllies.length];
		double minThreatHealth = Double.MAX_VALUE;
		double highestAtk = 0;
		MapLocation weakestThreat = null;
		for (RobotInfo enemy : nearbyEnemies) {
			// TODO: take turret min range into account here.
			if (enemy.type == RobotType.ARCHON || enemy.type == RobotType.ZOMBIEDEN || enemy.type == RobotType.SCOUT) {
				continue;
			}
			int distSq = enemy.location.distanceSquaredTo(curLoc);
			if (distSq <= enemy.type.attackRadiusSquared) {
				numCanShootUs++;

				highestAtk = Math.max(highestAtk, enemy.attackPower);

				if (distSq <= atkRangeSq) {
					double health = enemy.health;
					if (enemy.team == zombies && type == RobotType.GUARD) {
						health /= GameConstants.GUARD_ZOMBIE_MULTIPLIER;
					}

					if (health <= minThreatHealth) {
						minThreatHealth = health;
						weakestThreat = enemy.location;
					}
				}

				for (int i = canShootThem.length; --i >= 0;) {
					RobotInfo ally = nearbyAllies[i];
					if (ally.type == RobotType.ARCHON || ally.type == RobotType.ZOMBIEDEN
							|| ally.type == RobotType.SCOUT) {
						continue;
					}

					if (ally.location.distanceSquaredTo(enemy.location) <= ally.type.attackRadiusSquared) {
						canShootThem[i] = true;
					}
				}
			}
		}

		if (numCanShootUs > 0) {
			int numCanShootThem = 0;
			for (int i = canShootThem.length; --i >= 0;) {
				if (canShootThem[i]) {
					numCanShootThem++;
				}
			}

			if ((rc.getHealth() > highestAtk * 3 || !rc.isInfected()) && numCanShootThem + 1 >= numCanShootUs) {
				// attack
				if (weakestThreat != null) {
					// pick one that's an immediate threat
					if (rc.isWeaponReady()) {
						rc.attackLocation(weakestThreat);
					}
					// even if we're on cooldown, no sense moving and screwing
					// our
					// delays. just stand still.
					rc.setIndicatorString(2, "we outnumber, shooting");
					return;
				} else {
					if (rc.isCoreReady()) {
						RobotInfo weakest = getWeakestThreat(nearbyEnemies);
						MapLocation weakestLoc = null;
						if (weakest != null) {
							weakestLoc = weakest.location;
						}
						if (weakestLoc != null) {
							Pathfinding.setTarget(weakestLoc, false, false);
							Pathfinding.pathfindToward();
						}
						rc.setIndicatorString(2, "we outnumber but no one to shoot, advancing");
					}
					return;
				}
			} else {
				// retreat
				if (rc.isCoreReady()) {
					retreat(nearbyEnemies);
				}
				rc.setIndicatorString(2, "we're outnumbered, running");
				return;
			}
		} else {
			// check if we can shoot anyone without moving
			RobotInfo weakest = getWeakestInRange(nearbyEnemies);
			if (weakest != null) {
				if (rc.isWeaponReady()) {
					rc.attackLocation(weakest.location);
				}
				rc.setIndicatorString(2, "we're untouchable, shooting");
				return;
			} else {
				// path toward the weakest person nearby
				if (rc.isCoreReady()) {
					weakest = getWeakest(nearbyEnemies);
					MapLocation weakestLoc = null;
					if (weakest != null) {
						weakestLoc = weakest.location;
					}
					if (weakestLoc != null) {
						Pathfinding.setTarget(weakestLoc, nearbyAllies.length > nearbyEnemies.length, false);
						Pathfinding.pathfindToward();
					}
					rc.setIndicatorString(2, "no one to shoot, advancing");
				}
				return;
			}

		}
	}

	private static void retreat(RobotInfo[] nearbyEnemies) throws GameActionException {
		boolean[] dirs = Util.dirsAwayFrom(nearbyEnemies, curLoc);
		for (int i = 0; i < Util.ACTUAL_DIRECTIONS.length; i++) {
			if (dirs[i]) {
				if (rc.canMove(Util.ACTUAL_DIRECTIONS[i])) {
					rc.move(Util.ACTUAL_DIRECTIONS[i]);
					return;
				}
			}
		}
	}

	// TODO: several of these methods are very similar. extract their
	// similarities somehow?
	public static RobotInfo getWeakestInRange(RobotInfo[] nearby) {
		RobotInfo result = null;
		double minHealth = Double.MAX_VALUE;
		for (int i = nearby.length; --i >= 0;) {
			RobotInfo enemy = nearby[i];
			if (enemy.location.distanceSquaredTo(curLoc) <= atkRangeSq) {
				double health = enemy.health;
				if (enemy.team == zombies && type == RobotType.GUARD) {
					health /= GameConstants.GUARD_ZOMBIE_MULTIPLIER;
				}
				if (health < minHealth) {
					minHealth = health;
					result = enemy;
				}
			}
		}
		return result;
	}

	public static RobotInfo getWeakest(RobotInfo[] nearby) {
		RobotInfo result = null;
		double minHealth = Double.MAX_VALUE;
		for (int i = nearby.length; --i >= 0;) {
			RobotInfo enemy = nearby[i];
			double health = enemy.health;
			if (enemy.team == zombies && type == RobotType.GUARD) {
				health /= GameConstants.GUARD_ZOMBIE_MULTIPLIER;
			}
			if (health < minHealth) {
				minHealth = health;
				result = enemy;
			}
		}
		return result;
	}

	private static RobotInfo getWeakestThreat(RobotInfo[] nearby) {
		RobotInfo result = null;
		double minHealth = Double.MAX_VALUE;
		for (int i = nearby.length; --i >= 0;) {
			RobotInfo enemy = nearby[i];
			if (enemy.location.distanceSquaredTo(curLoc) > enemy.type.attackRadiusSquared) {
				continue;
			}
			double health = enemy.health;
			if (enemy.team == zombies && type == RobotType.GUARD) {
				health /= GameConstants.GUARD_ZOMBIE_MULTIPLIER;
			}
			if (health < minHealth) {
				minHealth = health;
				result = enemy;
			}
		}
		return result;
	}

}
