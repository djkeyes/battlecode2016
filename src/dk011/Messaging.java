package dk011;

import battlecode.common.GameActionException;
import battlecode.common.Signal;

public class Messaging extends BaseHandler {

	public static void sendMessage(Message m, int broadcastRadiusSq) throws GameActionException {
		int[] encoded = m.encodeMessage();
		rc.broadcastMessageSignal(encoded[0], encoded[1], broadcastRadiusSq);
	}

	public static void sendDenDeathMessage() throws GameActionException {
		rc.broadcastSignal(MapEdgesReceiver.getMinAllMapRadius());
	}

	public static void receiveAndProcessMessages() {
		Signal[] signals = rc.emptySignalQueue();

		EnemyUnitReceiver.resetRound();

		for (Signal s : signals) {
			Message.decodeMessage(s, us);
		}
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
