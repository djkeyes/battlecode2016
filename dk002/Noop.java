package dk002;

import battlecode.common.Clock;

public class Noop {

	public static void run() {
		while(true){
			Clock.yield();
		}
	}

}
