package team292;

import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

public class EnemyUnitReporter extends EnemyUnitReceiver {

	public static void reportEnemyUnits(RobotInfo[] nearbyHostiles, int defaultMessageRadius, int turretMessageRadius,
			boolean okayToShoutDens) throws GameActionException {
		// if there are a lot of enemies, this could get costly. consider
		// terminating early if we're running out of bytecodes.
		int allMapRadius = 0;
		if (okayToShoutDens) {
			allMapRadius = MapEdgesReceiver.getMinAllMapRadius();
		}
		for (int i = 0; i < nearbyHostiles.length; i++) {
			if (rc.getMessageSignalCount() == GameConstants.MESSAGE_SIGNALS_PER_TURN) {
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
				if (cur.type == RobotType.TURRET) {
					Messaging.sendMessage(m, turretMessageRadius);
				} else {
					Messaging.sendMessage(m, defaultMessageRadius);
				}
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

	public static void announceDenDeathMessage(MapLocation denLoc, int broadcastRadiusSq) throws GameActionException {
		Messaging.sendMessage(new DenDeathMessage(denLoc), broadcastRadiusSq);
	}

	public static void reportDenDeaths(int broadcastRadiusSq) throws GameActionException {

		DoublyLinkedList.DoublyLinkedListNode<MapLocation> cur = zombieDenLocations.head;
		while (cur != null) {
			DoublyLinkedList.DoublyLinkedListNode<MapLocation> next = cur.next;
			if (cur.data.distanceSquaredTo(curLoc) <= sensorRangeSq) {
				RobotInfo supposedDen = rc.senseRobotAtLocation(cur.data);
				if (supposedDen == null || supposedDen.type != RobotType.ZOMBIEDEN) {
					removeDen(cur);
					announceDenDeathMessage(cur.data, broadcastRadiusSq);
				}
			}
			cur = next;
		}

	}

	public static class DenDeathMessage extends Message {

		private MapLocation loc;

		public DenDeathMessage(MapLocation denLoc) {
			loc = denLoc;
		}

		@Override
		public int[] encodeMessage() {
			appendData(loc.x, 581);
			appendData(loc.y, 581);
			return super.encodeMessage();
		}

		@Override
		protected long getMessageOrdinal() {
			return PRECISE_DEN_DEATH_MESSAGE;
		}

		public static void processDenDeathMessage(long allBits) throws GameActionException {
			setBits(allBits);
			int y = (int) consumeData(581);
			int x = (int) consumeData(581);

			if (denReferences[x][y] != null) {
				removeDen(denReferences[x][y]);
			}
		}
	}
}
