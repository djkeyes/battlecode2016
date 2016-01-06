package team155;

import battlecode.common.*;

import java.util.Random;

// OKAY HERE'S THE PLAN
// THIS IS GONNA BE A PRETTY SIMPLE IMPLEMENTATION IN WHICH WE JUST MAKE SCOUTS AND TURRETS
// TURRETS HAVE THE HIGHEST DPS IN THE GAME, AND SCOUTS HAVE A BUNCH OF RANGE AND COMPUTATIONAL POWER
// SO SCOUTS CAN CALCULATED AND BROADCAST THINGS TO KILL, AND THEN TURRETS CAN KILL THEM
public class RobotPlayer {

	public static void run(RobotController rc) {
		while (true) {
			try {
				// TODO(daniel): all of these classes have a static method that
				// more-or-less does the same thing. It would be nice to use
				// inheritance
				// to enforce this, but java doesn't have abstract static
				// methods
				// (plus
				// that seems like it would have a bytecode impact). Maybe
				// there's a
				// way
				// to enfore this pattern using an annotation processor?
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
					Noop.run();
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
