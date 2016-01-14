package dk007;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

public class Micro extends BaseHandler {

	// TODO: none of this takes into account min turret range
	// with lone turrets, your best strategy is to rush them and get under their
	// minimum range
	public static void doMicro(RobotInfo[] nearbyAllies, RobotInfo[] nearbyEnemies, SignalContents[] decodedSignals)
			throws GameActionException {
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
		boolean canWeOutrangeAnyThreats = false;
		for (RobotInfo enemy : nearbyEnemies) {
			// TODO: take turret min range into account here.
			if (enemy.type == RobotType.ARCHON || enemy.type == RobotType.ZOMBIEDEN || enemy.type == RobotType.SCOUT) {
				continue;
			}
			int distSq = enemy.location.distanceSquaredTo(curLoc);
			if (distSq <= enemy.type.attackRadiusSquared) {
				numCanShootUs++;

				highestAtk = Math.max(highestAtk, enemy.attackPower);

				if (!canWeOutrangeAnyThreats && atkRangeSq >= enemy.type.attackRadiusSquared) {
					canWeOutrangeAnyThreats = true;
				}

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
					} else if (canWeOutrangeAnyThreats) {
						// if we're on cooldown, we can still move
						// for every unit (except turrets), moving increases the
						// weapon delay to AT MOST 1. but that's fine for us,
						// because it's already larger than that.
						if (rc.isCoreReady()) {
							retreat(nearbyEnemies, false);
						}
					}
					return;
				} else {
					if (rc.isCoreReady()) {
						RobotInfo weakest = getWeakestThreat(nearbyEnemies);
						MapLocation weakestLoc = null;
						if (weakest == null) {
							weakestLoc = getWeakestThreat(decodedSignals);
						} else {
							weakestLoc = weakest.location;
						}
						if (weakestLoc != null) {
							Pathfinding.setTarget(weakestLoc, false, false, false);
							Pathfinding.pathfindToward();
						}
					}
					return;
				}
			} else {
				// retreat
				if (rc.isCoreReady()) {
					retreat(nearbyEnemies, false);
				}
				return;
			}
		} else {
			// check if we can shoot anyone without moving
			RobotInfo weakest = getWeakestInRange(nearbyEnemies);
			if (weakest != null) {
				if (rc.isWeaponReady()) {
					rc.attackLocation(weakest.location);
				}
				return;
			} else {
				// path toward the weakest person nearby
				if (rc.isCoreReady()) {
					weakest = getWeakest(nearbyEnemies);
					MapLocation weakestLoc = null;
					if (weakest == null) {
						weakestLoc = getWeakest(decodedSignals);
					} else {
						weakestLoc = weakest.location;
					}
					if (weakestLoc != null) {
						Pathfinding.setTarget(weakestLoc, nearbyAllies.length > nearbyEnemies.length, false, false);
						Pathfinding.pathfindToward();
					}
				}
				return;
			}

		}
	}

	public static boolean retreat(RobotInfo[] nearbyEnemies, boolean clearRubbleAggressively)
			throws GameActionException {
		boolean[] isAwayFromEnemy = Util.dirsAwayFrom(nearbyEnemies, curLoc);

		Direction dirToMove = null;
		Direction dirToDig = null;
		Direction unsafeDirToMove = null;
		double minRubble = Double.MAX_VALUE;
		for (int i = Util.RANDOM_DIRECTION_PERMUTATION.length; --i >= 0;) {
			Direction d = Util.RANDOM_DIRECTION_PERMUTATION[i];
			if (isAwayFromEnemy[Util.dirToInt(d)]) {
				MapLocation next = curLoc.add(d);
				double rubble = rc.senseRubble(next);
				if (rubble >= GameConstants.RUBBLE_SLOW_THRESH && rc.senseRobotAtLocation(next) == null) {
					if (rubble < minRubble) {
						minRubble = rubble;
						dirToDig = d;
					}
				} else if (rc.canMove(d)) {
					// if there's a free spot, take advantage of it
					// immediately
					dirToMove = d;
					break;
				}
			} else if (unsafeDirToMove == null && rc.canMove(d)) {
				unsafeDirToMove = d;
			}
		}

		if (dirToMove != null || dirToDig != null) {
			if (dirToMove != null) {
				rc.move(dirToMove);
			} else if (dirToDig != null) {
				if (clearRubbleAggressively
						|| (minRubble >= GameConstants.RUBBLE_OBSTRUCTION_THRESH || Rubble
								.betterToClearRubble(minRubble))) {
					rc.clearRubble(dirToDig);
				} else {
					rc.move(dirToDig);
				}
			}
		} else if (unsafeDirToMove != null) {
			// better to move than stand still
			rc.move(unsafeDirToMove);
		}

		return true;
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

	private static MapLocation getWeakest(SignalContents[] decodedSignals) {
		MapLocation result = null;
		double minHealth = Double.MAX_VALUE;
		for (int i = decodedSignals.length; --i >= 0;) {
			SignalContents cur = decodedSignals[i];
			double health = cur.health;
			if (cur.isZombie && type == RobotType.GUARD) {
				health /= GameConstants.GUARD_ZOMBIE_MULTIPLIER;
			}
			if (health < minHealth) {
				minHealth = health;
				result = new MapLocation(cur.x, cur.y);
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

	private static MapLocation getWeakestThreat(SignalContents[] decodedSignals) {
		MapLocation result = null;
		double minHealth = Double.MAX_VALUE;
		for (int i = decodedSignals.length; --i >= 0;) {
			SignalContents cur = decodedSignals[i];
			MapLocation loc = new MapLocation(cur.x, cur.y);
			if (loc.distanceSquaredTo(curLoc) > cur.type.attackRadiusSquared) {
				continue;
			}
			double health = cur.health;
			if (cur.isZombie && type == RobotType.GUARD) {
				health /= GameConstants.GUARD_ZOMBIE_MULTIPLIER;
			}
			if (health < minHealth) {
				minHealth = health;
				result = loc;
			}
		}
		return result;
	}

}
