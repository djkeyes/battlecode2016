package dk010;

import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

public interface Strategy {

	public RobotType getNextToBuild(RobotInfo[] nearbyAllies);

	public void incrementNextToBuild();
}
