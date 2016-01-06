package team155;

public class Util {

	// public static int[] encodeSignal(double health, int id, boolean canDodge)
	// {
	// int[] result = new int[2];
	// result[0] = id;
	// float fHealth = (float) health;
	// if (canDodge) {
	// fHealth = -fHealth;
	// }
	// result[1] = Float.floatToIntBits(fHealth);
	// return result;
	// }
	//
	// public static class SignalContents {
	// public double health;
	// public int id;
	// public boolean canDodge;
	// }
	//
	// public static SignalContents decodeSignal(int[] signal) {
	// SignalContents result = new SignalContents();
	// result.id = signal[0];
	// float fHealth = Float.intBitsToFloat(signal[1]);
	// result.canDodge = fHealth < 0;
	// if (fHealth < 0) {
	// fHealth = -fHealth;
	// }
	// result.health = fHealth;
	// return result;
	// }
}
