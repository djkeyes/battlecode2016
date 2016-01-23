package dk011;

import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

public class EnemyUnitReporter extends EnemyUnitReceiver {

	public static void reportEnemyUnits(RobotInfo[] nearbyHostiles, int defaultMessageRadius, boolean okayToShoutDens)
			throws GameActionException {
		// if there are a lot of enemies, this could get costly. consider
		// terminating early if we're running out of bytecodes.
		int allMapRadius = 0;
		if (okayToShoutDens) {
			allMapRadius = MapEdgesReceiver.getMinAllMapRadius();
		}
		for (int i = 0; i < nearbyHostiles.length; i++) {
			if(rc.getMessageSignalCount() == GameConstants.MESSAGE_SIGNALS_PER_TURN){
				// :(
				break;
			}
			RobotInfo cur = nearbyHostiles[i];
			int health = (int) cur.health;
			int coreDelay = (int) cur.coreDelay;
			if (cur.type == RobotType.TURRET) {
				coreDelay += GameConstants.TURRET_TRANSFORM_DELAY;
			}
			coreDelay = Math.min(coreDelay, EnemyUnitMessage.MAX_CORE_DELAY - 1);

			int type = cur.type.ordinal();
			// these values range from 0 to 200.
			int locOffsetX = cur.location.x - curLoc.x + ActualGameConstants.MAP_MAX_WIDTH;
			int locOffsetY = cur.location.y - curLoc.y + ActualGameConstants.MAP_MAX_HEIGHT;
			boolean zombism = cur.team == zombies;

			if (cur.type == RobotType.ZOMBIEDEN) {
				if (!tryAddDen(cur.location)) {
					continue;
				}
			} else if (cur.type == RobotType.TURRET) {
				addTurret(cur.location, curTurn + APPROX_TURRET_TRANSFORM_DELAY);
			}

			Message m = new EnemyUnitMessage(zombism, locOffsetX, locOffsetY, type, health, coreDelay, curTurn);
			if (!okayToShoutDens || cur.type != RobotType.ZOMBIEDEN) {
				Messaging.sendMessage(m, defaultMessageRadius);
			} else {
				Messaging.sendMessage(m, allMapRadius);
			}

		}
	}

	public static DoublyLinkedList.DoublyLinkedListNode<MapLocation> rereportZombieDens(
			DoublyLinkedList.DoublyLinkedListNode<MapLocation> startingNode, int broadcastRadius)
			throws GameActionException {

		DoublyLinkedList.DoublyLinkedListNode<MapLocation> cur = startingNode;
		int type = RobotType.ZOMBIEDEN.ordinal();
		boolean zombism = true;
		int health = (int) RobotType.ZOMBIEDEN.maxHealth;
		int coreDelay = EnemyUnitMessage.MAX_CORE_DELAY - 1;
		while (cur != null && rc.getMessageSignalCount() < GameConstants.MESSAGE_SIGNALS_PER_TURN) {
			// these values range from 0 to 200.
			int locOffsetX = cur.data.x - curLoc.x + ActualGameConstants.MAP_MAX_WIDTH;
			int locOffsetY = cur.data.y - curLoc.y + ActualGameConstants.MAP_MAX_HEIGHT;

			Message m = new EnemyUnitMessage(zombism, locOffsetX, locOffsetY, type, health, coreDelay, curTurn);
			Messaging.sendMessage(m, broadcastRadius);

			cur = cur.next;
		}

		return cur;
	}

	public static final int DEN_DEATH_REPORT_DISTANCE = 8;

	public static void maybeAnnounceDenDeath(MapLocation denLoc) {
		// only announce the den is dead if we're close enough
		// that way, even if someone misinterprets our message and removes the
		// wrong den, they're still in the ballpark

		if (curLoc.distanceSquaredTo(denLoc) < DEN_DEATH_REPORT_DISTANCE) {
			// TODO
		}
	}
}
