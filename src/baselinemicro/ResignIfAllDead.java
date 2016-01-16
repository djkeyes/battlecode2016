package baselinemicro;

import battlecode.common.Clock;

public class ResignIfAllDead extends BaseHandler {

	public static void run() {
		while (true) {
			int numBots = rc.getRobotCount();
			if (numBots == 1) {
				rc.resign();
			}
			Clock.yield();
		}

	}

}
