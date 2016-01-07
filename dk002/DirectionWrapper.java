package dk002;

import battlecode.common.Direction;

// static class to store some direction constants
// static class variables are initialized the first time they are accessed, so sequestering this into a separate class
// saves bytecodes unless the unit actually needs it
// that being said, this could be premature bytecode optimization. feel free to refactor into to whatever works best.
public class DirectionWrapper {

	public static final Direction[] ACTUAL_DIRECTIONS = { Direction.NORTH, Direction.NORTH_EAST, Direction.EAST,
			Direction.SOUTH_EAST, Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST };

	public static final Direction[] CARDINAL_DIRECTIONS = { Direction.NORTH, Direction.EAST, Direction.SOUTH,
			Direction.WEST, };
	public static final Direction[] UN_CARDINAL_DIRECTIONS = { Direction.NORTH_EAST, Direction.SOUTH_EAST,
			Direction.SOUTH_WEST, Direction.NORTH_WEST, };
}
