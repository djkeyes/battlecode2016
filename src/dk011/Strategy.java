package dk011;

import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

public interface Strategy {

	void incrementNextToBuild();

	RobotType getNextToBuild(RobotInfo[] curAlliesInSight);

}
