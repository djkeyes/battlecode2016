package team292;

import battlecode.common.GameActionException;
import battlecode.common.RobotType;
import battlecode.common.Signal;

public class Messaging extends BaseHandler {

	public static int maxMessagesToProcessPerTurn;

	public static void init() {
		if (rc.getType() == RobotType.ARCHON || rc.getType() == RobotType.SCOUT) {
			maxMessagesToProcessPerTurn = 70;
		} else {
			maxMessagesToProcessPerTurn = 30;
		}
	}

	public static void sendMessage(Message m, int broadcastRadiusSq) throws GameActionException {
		int[] encoded = m.encodeMessage();
		rc.broadcastMessageSignal(encoded[0], encoded[1], broadcastRadiusSq);
	}

	public static void sendDenDeathBasicSignal() throws GameActionException {
		rc.broadcastSignal(MapEdgesReceiver.getMinAllMapRadius());
	}

	public static void receiveAndProcessMessages() throws GameActionException {
		Signal[] signals = rc.emptySignalQueue();

//		rc.setIndicatorString(0, "starting to process " + signals.length + " messages at bc=" + Clock.getBytecodeNum());

		EnemyUnitReceiver.resetRound();

		int messagesProcessed = 0;
		for (Signal s : signals) {
			Message.decodeMessage(s, us);
			if (++messagesProcessed >= maxMessagesToProcessPerTurn) {
				break;
			}
		}
		// rc.setIndicatorString(2, "finished processing " + messagesProcessed +
		// "/" + signals.length + " messages at bc="
		//			+ Clock.getBytecodeNum());
	}

	public static void receiveAndProcessDestinyMessage() {
		Signal s = rc.readSignal();

		// if there wasn't actually a destiny message, this erases something
		// from the queue. oh well.
		if (s != null && s.getTeam() == us && s.getMessage() != null) {
			long allBits = Message.intsToLong(s.getMessage());
			int messageType = (int) (allBits % Message.NUM_MESSAGE_TYPES);
			if (messageType == Message.DESTINY_MESSAGE) {
				allBits /= Message.NUM_MESSAGE_TYPES;
				DestinyReceiver.processMessage(allBits);
			}
		}
	}

}
