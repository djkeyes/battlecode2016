package team292;

import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

// utility methods for selecting units with certain qualities from a list
public class UnitSelection extends BaseHandler {

	public static RobotInfo getMostDamagedNonArchon(RobotInfo[] nearbyAllies, int thresholdDistSq) {
		RobotInfo result = null;
		double maxDamage = 0;
		for (int i = nearbyAllies.length; --i >= 0;) {
			RobotInfo robot = nearbyAllies[i];
			if (robot.type == RobotType.ARCHON) {
				continue;
			}
			if (robot.location.distanceSquaredTo(curLoc) <= thresholdDistSq) {
				double damage = robot.type.maxHealth - robot.health;
				if (damage > maxDamage) {
					maxDamage = damage;
					result = robot;
				}
			}

		}
		return result;
	}

}