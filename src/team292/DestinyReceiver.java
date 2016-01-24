package team292;

public class DestinyReceiver {

	public static final int NUM_DESTINIES = 2;
	public static final int DEFAULT = 0;
	public static final int PAIRED_TURRET_SCOUT = 1;

	public static int destiny = DEFAULT;
	public static int friendId = DEFAULT;

	public static void processMessage(long bits) {
		destiny = (int) (bits % NUM_DESTINIES);
		friendId = (int) (bits / (long) NUM_DESTINIES);
	}

}
