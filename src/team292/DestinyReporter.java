package team292;

import battlecode.common.GameActionException;

public class DestinyReporter extends DestinyReceiver {

	public static void setDestiny(int destiny, int yourInt) throws GameActionException {
		Messaging.sendMessage(new DestinyMessage(destiny, yourInt), 2);
	}

	private static class DestinyMessage extends Message {
		@Override
		protected long getMessageOrdinal() {
			return DESTINY_MESSAGE;
		}

		private int destiny;
		private int friendId;

		public DestinyMessage(int destiny, int friendId) {
			this.destiny = destiny;
			this.friendId = friendId;
		}

		public int[] encodeMessage() {
			appendData(friendId, Integer.MAX_VALUE);
			appendData(destiny, NUM_DESTINIES);
			return super.encodeMessage();
		}
	}
}
