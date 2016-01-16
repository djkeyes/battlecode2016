package microtest;

import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;

public class Micro extends BaseHandler {

	// summery of variables:
	// for enemies:
	// weapon delay and health x if they can shoot us or not
	// for allies:
	// weapon delay and health x if they can shoot the people who can shoot us,
	// or not
	// for us:
	// weapon delay, health
	// misc stats:
	// total enemies, total allies, total enemies that can shoot us, total
	// allies that can shoot the people who can shoot us

	public static double tWdIr, tHIr, tWdOor, tHOor, aWdIr, aHIr, aWdOor, aHOor;
	public static double myH, myWd, tTotal, aTotal, tTotalIr, aTotalIr;

	public static void readWeights() {
		String weightString;
		if (us.equals(Team.A)) {
			weightString = System.getProperty("bc.testing.team-a-weights");
		} else {
			weightString = System.getProperty("bc.testing.team-b-weights");
		}

		String[] weightStrings = weightString.split(",");

		int i = 0;
		tWdIr = Double.parseDouble(weightStrings[i++]);
		tHIr = Double.parseDouble(weightStrings[i++]);
		tWdOor = Double.parseDouble(weightStrings[i++]);
		tHOor = Double.parseDouble(weightStrings[i++]);
		aWdIr = Double.parseDouble(weightStrings[i++]);
		aHIr = Double.parseDouble(weightStrings[i++]);
		aWdOor = Double.parseDouble(weightStrings[i++]);
		aHOor = Double.parseDouble(weightStrings[i++]);
		myWd = Double.parseDouble(weightStrings[i++]);
		myH = Double.parseDouble(weightStrings[i++]);
		tTotal = Double.parseDouble(weightStrings[i++]);
		aTotal = Double.parseDouble(weightStrings[i++]);
		tTotalIr = Double.parseDouble(weightStrings[i++]);
		aTotalIr = Double.parseDouble(weightStrings[i++]);
	}

	private static MapLocation weakestThreat;

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

		int numCanShootUs = 0;
		boolean[] canShootThem = new boolean[nearbyAllies.length];
		double minThreatHealth = Double.MAX_VALUE;
		double highestAtk = 0;
		weakestThreat = null;

		double dangerLevel = 0;
		boolean canWeakestThreatShootUs = false;
		for (RobotInfo enemy : nearbyEnemies) {
			// TODO: take turret min range into account here.
			if (enemy.type == RobotType.ARCHON || enemy.type == RobotType.ZOMBIEDEN || enemy.type == RobotType.SCOUT) {
				continue;
			}
			int distSq = enemy.location.distanceSquaredTo(curLoc);
			double health = normalizeHealth(enemy.health, enemy.team);

			if (distSq <= enemy.type.attackRadiusSquared) {
				numCanShootUs++;
				dangerLevel += tHIr * health;
				dangerLevel += tWdIr * enemy.weaponDelay;

				highestAtk = Math.max(highestAtk, enemy.attackPower);

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
			} else {
				// too far away
				dangerLevel += tHOor * health;
				dangerLevel += tWdOor * enemy.weaponDelay;
			}

			if (distSq <= atkRangeSq) {
				// if the previous enemies couldn't shoot us, but this one can,
				// reprioritize
				boolean canThisThreatShootUs = distSq <= enemy.type.attackRadiusSquared;
				boolean smaller = health <= minThreatHealth;

				// aghhh, boolean logic
				if ((canWeakestThreatShootUs && (canThisThreatShootUs || smaller)) || (canThisThreatShootUs && smaller)) {
					minThreatHealth = health;
					weakestThreat = enemy.location;
				}
				if (canThisThreatShootUs) {
					canWeakestThreatShootUs = true;
				}
			}
		}

		if (numCanShootUs > 0) {
			int numCanShootThem = 0;
			for (int i = canShootThem.length; --i >= 0;) {
				if (canShootThem[i]) {
					numCanShootThem++;

					dangerLevel += aHIr * nearbyAllies[i].health;
					dangerLevel += aWdIr * nearbyAllies[i].weaponDelay;
				} else {
					dangerLevel += aHOor * nearbyAllies[i].health;
					dangerLevel += aWdOor * nearbyAllies[i].weaponDelay;
				}
			}

			// global stats
			dangerLevel += tTotal * nearbyEnemies.length + tTotalIr * numCanShootUs + aTotal
					* (nearbyAllies.length + 1) + aTotalIr * (numCanShootThem + 1);
			// unit stats
			dangerLevel += myH * rc.getHealth() + myWd * rc.getWeaponDelay();

			if ((rc.getHealth() > highestAtk * 3 || !rc.isInfected()) && dangerLevel <= 0) {
				// attack
				if (weakestThreat != null) {
					// pick one that's an immediate threat
					if (rc.isWeaponReady()) {
						rc.attackLocation(weakestThreat);
					}
					// even if we're on cooldown, no sense moving and screwing
					// our delays. just stand still.
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
					}
					return;
				}
			} else {
				// retreat
				if (rc.isCoreReady()) {
					retreat(nearbyEnemies);
				}
				return;
			}
		} else {
			// check if we can shoot anyone without moving
			MapLocation weakest = weakestThreat;
			if (weakest != null) {
				if (rc.isWeaponReady()) {
					rc.attackLocation(weakest);
				}
				return;
			} else {
				// path toward the weakest person nearby
				if (rc.isCoreReady()) {
					weakest = getWeakest(nearbyEnemies);
					if (weakest != null) {
						Pathfinding.setTarget(weakest, nearbyAllies.length > nearbyEnemies.length, false);
						Pathfinding.pathfindToward();
					}
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

	private static double normalizeHealth(double health, Team team) {
		if (team == zombies && type == RobotType.GUARD) {
			health /= GameConstants.GUARD_ZOMBIE_MULTIPLIER;
		}
		return health;
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

	public static MapLocation getWeakest(RobotInfo[] nearby) {
		MapLocation result = null;
		double minHealth = Double.MAX_VALUE;
		for (int i = nearby.length; --i >= 0;) {
			RobotInfo enemy = nearby[i];
			double health = enemy.health;
			if (enemy.team == zombies && type == RobotType.GUARD) {
				health /= GameConstants.GUARD_ZOMBIE_MULTIPLIER;
			}
			if (health < minHealth) {
				minHealth = health;
				result = enemy.location;
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
