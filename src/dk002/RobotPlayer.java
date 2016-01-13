package dk002;

import java.util.Random;

import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;

// OKAY HERE'S THE PLAN
// THIS PACKAGE IS GONNA TRY OUT A TOTALLY DIFFERENT STRATEGY:
// VIPER/SCOUT ZOMBIE KAMAKAZE
// (IT MIGHT ALSO BE SMART TO IMPLEMENT A SOLDIER/GUARD STRATEGY, JUST FOR REFERENCE. ULTIMATELY, WE'LL PROBABLY WANT TO COMBINE SEVERAL STRATEGIES.)
// THIS WILL ALSO INCLUDE SOME PATHFINDING. ARCHONS PATHFIND TOWARD EACH OTHER. GUARDS PATHFIND TOWARD ARCHONS. KAMAKAZES PATHFIND TOWARD ENEMY ARCHONS.

public class RobotPlayer {

	public static RobotController rc;

	// why is this here and not in, for examle, Util?
	// because it's seeded with the rc.getID(), which isn't available during
	// static initialization! (unless you want to gamble with static class
	// initialization order...)
	public static Random gen;

	public static void run(RobotController rc) {
		RobotPlayer.rc = rc;
		gen = new Random(rc.getID());
		while (true) {
			try {
				// TODO(daniel): all of these classes have a static method that
				// more-or-less does the same thing. It would be nice to use
				// inheritance to enforce this, but java doesn't have abstract
				// static methods (plus that seems like it would have a bytecode
				// impact). Maybe there's a way to enforce this pattern using an
				// annotation processor?
				switch (rc.getType()) {
				case ARCHON:
					Archon.run(rc);
					break;
				case GUARD:
					Noop.run();
					break;
				case SCOUT:
					Scout.run(rc);
					break;
				case SOLDIER:
					Noop.run();
					break;
				case TTM:
					Turret.run(rc);
					break;
				case TURRET:
					Turret.run(rc);
					break;
				case VIPER:
					Viper.run(rc);
					break;
				default:
					break;
				}
			} catch (GameActionException e) {
				// ignore and continue
				// should probably comment out the stack dump in final
				// submission, just in case
				e.printStackTrace();
				Clock.yield();
			}
		}
	}
}
