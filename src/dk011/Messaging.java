package dk011;

import battlecode.common.GameActionException;
import battlecode.common.Signal;

public class Messaging extends BaseHandler {

	public static void sendMessage(Message m, int broadcastRadiusSq) throws GameActionException {
		int[] encoded = m.encodeMessage();
		rc.broadcastMessageSignal(encoded[0], encoded[1], broadcastRadiusSq);
	}

	public static void receiveAndProcessMessages() {
		Signal[] signals = rc.emptySignalQueue();

		for (Signal s : signals) {
			Message.decodeMessage(s, us);
		}
	}
}
