package dk011;

import battlecode.common.Signal;
import battlecode.common.Team;

/**
 * Super-class to try to unify some common things used by messaging. This API is
 * a little restricted, because it actively avoids using any Java Collections in
 * order to save bytecodes.
 * 
 * Subclasses should provide both a means to encode and decode messages. While
 * in the real world, this is cluttered, confusing, and has many edge cases
 * (what if someone tries to re-send a received message? ans for this: don't
 * fucking try it), this should hopefully keep coupled logic within the same
 * file, for readability.
 * 
 * As a result, most subclasses should have two constructors: a constructor
 * chained to Message(long), which decodes a message using consumeData(), and a
 * constructor that takes lots of parameters and encodes a message using
 * appendData() in the opposite order. Callers should decode a message using
 * Message.decodeMessage() and encode the int[] part of a message by creating an
 * instance of their favorite message type and calling encodeMessage().
 */
public abstract class Message {

	// ideally, we'd have some kind of enum that provided a particular class
	// constructor given a message ordinal. but imagine now many bytecodes that
	// would cost :S
	public static final int NUM_MESSAGE_TYPES = 5;
	protected static final int MAP_EDGE_MESSAGE = 0;
	protected static final int ENEMY_UNIT_MESSAGE = 1;
	protected static final int FREE_STUFF_MESSAGE = 2;
	protected static final int DESTINY_MESSAGE = 3;
	protected static final int ARCHON_LOCATION_MESSAGE = 4;

	protected long allBits;

	protected Message() {

	}

	public static void decodeMessage(Signal signal, Team us) {
		if (signal.getTeam() != us) {
			EnemyUnitMessage.processEnemyMessage(signal);
			return;
		}
		if (signal.getMessage() == null) {
			// this is a den death message
			EnemyUnitReceiver.processDenDeath(signal.getLocation());
			return;
		}

		long allBits = intsToLong(signal.getMessage());

		// System.out.println("recieve message with ints "
		// + String.format("%32s",
		// Integer.toBinaryString(signal.getMessage()[0])).replace(' ', '0')
		// + String.format("%32s",
		// Integer.toBinaryString(signal.getMessage()[1])).replace(' ', '0'));
		// System.out.println("recieve message with bits "
		// + String.format("%64s", Long.toBinaryString(allBits)).replace(' ',
		// '0'));
		int messageType = (int) (allBits % NUM_MESSAGE_TYPES);
		allBits /= NUM_MESSAGE_TYPES;

		switch (messageType) {
		case MAP_EDGE_MESSAGE:
			MapEdgeMessage.processMessage(allBits);
			break;
		case ENEMY_UNIT_MESSAGE:
			EnemyUnitMessage.processMessage(allBits, signal);
			break;
		case FREE_STUFF_MESSAGE:
			FreeStuffMessage.processMessage(allBits);
			break;
		case DESTINY_MESSAGE:
			break;
		case ARCHON_LOCATION_MESSAGE:
			ArchonReporter.ArchonLocationMessage.processMessage(allBits);
			break;
		}

	}

	private static long processedBits;

	protected static void setBits(long allBits) {
		processedBits = allBits;
	}

	/**
	 * consumes some more bits off the current message, set by setBits(long)
	 */
	protected static long consumeData(long dataSize) {
		long result = processedBits % dataSize;
		processedBits /= dataSize;
		return result;
	}

	/**
	 * add some bits to the current message
	 */
	protected void appendData(long data, long dataSize) {
		allBits = allBits * dataSize + data;
	}

	public int[] encodeMessage() {
		appendData(getMessageOrdinal(), NUM_MESSAGE_TYPES);
		// System.out.println("sending message with bits "
		// + String.format("%64s", Long.toBinaryString(allBits)).replace(' ',
		// '0'));
		// System.out.println("sending message with ints "
		// + String.format("%32s",
		// Integer.toBinaryString(longToInts(allBits)[0])).replace(' ', '0')
		// + String.format("%32s",
		// Integer.toBinaryString(longToInts(allBits)[1])).replace(' ', '0'));
		return longToInts(allBits);
	}

	/**
	 * @return a number corresponding to this message type. This number sould be
	 *         declared as a constant in Message.java
	 */
	protected abstract long getMessageOrdinal();

	final static long intsToLong(int[] intPair) {
		long longBits = intPair[0];
		longBits = (longBits << 32) | ((long) intPair[1] & 0xFFFFFFFFL);
		return longBits;
	}

	private final static int[] longToInts(long longBits) {
		return new int[] { (int) (longBits >>> 32), (int) longBits };
	}
}
