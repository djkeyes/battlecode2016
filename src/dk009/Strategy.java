package dk009;

import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

public interface Strategy {

	public RobotType getNextToBuild(RobotInfo[] allies);

	public void incrementNextToBuild();
}
