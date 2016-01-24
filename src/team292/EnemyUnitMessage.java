package team292;

import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotType;
import battlecode.common.Signal;

// to be honest, this class should probably be declared in EnemyUnitReceiver, since it access so may things in that class
public class EnemyUnitMessage extends Message {

	@Override
	protected long getMessageOrdinal() {
		return ENEMY_UNIT_MESSAGE;
	}

	public static final int MAX_HEALTH = 1501; // big zombies at level 9
	public static final int MAX_CORE_DELAY = 101;
	public static final int NUM_TYPES = RobotType.values().length;
	public static final int MAX_NUM_ROUNDS = 3001;
	public static final int MAX_X_OFFSET = 2 * ActualGameConstants.MAP_MAX_WIDTH + 1;
	public static final int MAX_Y_OFFSET = 2 * ActualGameConstants.MAP_MAX_HEIGHT + 1;
	private boolean isZombie;
	private int xLoc, yLoc, typeOrdinal, health, coreDelay, curTurn;

	/******** SENDING *********/

	public EnemyUnitMessage(boolean isZombie, int xLoc, int yLoc, int typeOrdinal, int health, int coreDelay,
			int curTurn) {
		this.isZombie = isZombie;
		this.xLoc = xLoc;
		this.yLoc = yLoc;
		this.typeOrdinal = typeOrdinal;
		this.health = health;
		this.coreDelay = coreDelay;
		this.curTurn = curTurn;
	}

	public int[] encodeMessage() {

		appendData(health, MAX_HEALTH);
		appendData(coreDelay, MAX_CORE_DELAY);
		appendData(curTurn, MAX_NUM_ROUNDS);
		appendData(typeOrdinal, NUM_TYPES);
		appendData(xLoc, MAX_X_OFFSET);
		appendData(yLoc, MAX_Y_OFFSET);
		appendData(isZombie ? 1 : 0, 2);

		return super.encodeMessage();
	}

	/******* RECEIVING ********/

	public static void processMessage(long allBits, Signal signal) {
		setBits(allBits);

		boolean isZombie = consumeData(2) > 0;
		int yLoc = (int) consumeData(MAX_Y_OFFSET);
		int xLoc = (int) consumeData(MAX_X_OFFSET);
		int typeOrdinal = (int) consumeData(NUM_TYPES);
		int curTurn = (int) consumeData(MAX_NUM_ROUNDS);
		int coreDelay = (int) consumeData(MAX_CORE_DELAY);
		int health = (int) consumeData(MAX_HEALTH);

		MapLocation baseLoc = signal.getLocation();
		MapLocation actualLoc = new MapLocation(xLoc + baseLoc.x - ActualGameConstants.MAP_MAX_WIDTH, yLoc + baseLoc.y
				- ActualGameConstants.MAP_MAX_HEIGHT);

		if (health < EnemyUnitReceiver.weakestBroadcastedEnemyHealth) {
			EnemyUnitReceiver.weakestBroadcastedEnemyHealth = health;
			EnemyUnitReceiver.weakestBroadcastedEnemy = actualLoc;
		}
		int distSq = BaseHandler.curLoc.distanceSquaredTo(actualLoc);
		if (distSq <= RobotType.TURRET.attackRadiusSquared && distSq >= GameConstants.TURRET_MINIMUM_RANGE
				&& health < EnemyUnitReceiver.weakestBroadcastedEnemyHealthInTurretRange) {
			EnemyUnitReceiver.weakestBroadcastedEnemyHealthInTurretRange = health;
			EnemyUnitReceiver.weakestBroadcastedEnemyInTurretRange = actualLoc;
		}

		// System.out
		// .println(String
		// .format("recieved enemy unit message, isZombie: %b, yLoc %d, xLoc %d, typeOrdinal %d, type %s, curTurn %d, coreDelay %d, health %d, actualLoc %s",
		// isZombie, yLoc, xLoc, typeOrdinal,
		// RobotType.values()[typeOrdinal].toString(), curTurn,
		// coreDelay, health, actualLoc.toString()));
		if (RobotType.values()[typeOrdinal] == RobotType.ZOMBIEDEN) {
			EnemyUnitReceiver.tryAddDen(actualLoc);
		} else if (RobotType.values()[typeOrdinal] == RobotType.TURRET) {
			EnemyUnitReceiver.addTurret(actualLoc, curTurn + EnemyUnitReporter.APPROX_TURRET_TRANSFORM_DELAY);

			if (distSq <= RobotType.TURRET.attackRadiusSquared && distSq >= GameConstants.TURRET_MINIMUM_RANGE
					&& health < EnemyUnitReceiver.weakestBroadcastedEnemyHealthInTurretRange) {
				EnemyUnitReceiver.weakestBroadcastedTurretHealthInTurretRange = health;
				EnemyUnitReceiver.weakestBroadcastedTurretInTurretRange = actualLoc;

				if (health < EnemyUnitReceiver.weakestBroadcastedTimestampedTurretHealthInTurretRange) {
					EnemyUnitReceiver.weakestBroadcastedTimestampedTurretHealthInTurretRange = health;
					EnemyUnitReceiver.weakestBroadcastedTimestampedTurretInTurretRange = actualLoc;
					EnemyUnitReceiver.weakestBroadcastedTimestampedTurretInTurretRangeTimestamp = curTurn
							+ EnemyUnitReceiver.APPROX_TURRET_TRANSFORM_DELAY;
				}
			}
		}

	}

	public static void processEnemyMessage(Signal signal) {
		MapLocation loc = signal.getLocation();

		int distSq = BaseHandler.curLoc.distanceSquaredTo(loc);

		if (distSq < EnemyUnitReceiver.weakestBroadcastedEnemyHealth) {
			EnemyUnitReceiver.closestHeardEnemyDistSq = distSq;
			EnemyUnitReceiver.closestHeardEnemy = loc;
		}
	}

}
