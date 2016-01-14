package team292;

import battlecode.common.RobotType;

public interface Strategy {

	public RobotType getNextToBuild();
	
	public void incrementNextToBuild();
}
