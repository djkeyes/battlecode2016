package team292;

public class MapEdgesReceiver extends BaseHandler {

	public static Integer minRow, maxRow, minCol, maxCol;
	public static Integer mapWidth, mapHeight;

	public static int getMinAllMapRadius() {
		int curX = curLoc.x;
		int curY = curLoc.y;

		int top, bottom, left, right;
		if (minRow != null) {
			top = minRow;
		} else if (maxRow != null) {
			// if this case is true, we necessarily don't know the height
			// (otherwise we would have filled in minRow in checkMapEdges())

			// origin is within [0, 500]
			top = Math.max(0, maxRow - ActualGameConstants.MAP_MAX_HEIGHT + 1);
		} else if (mapHeight != null) {
			top = Math.max(0, curY - mapHeight + 1);
		} else {
			top = Math.max(0, curY - ActualGameConstants.MAP_MAX_HEIGHT + 1);
		}

		int maxMaxRow = 500 + ActualGameConstants.MAP_MAX_HEIGHT - 1;
		if (maxRow != null) {
			bottom = maxRow;
		} else if (minRow != null) {
			// origin is within [0, 500], ergo this can't be higher than 579
			bottom = Math.min(maxMaxRow, minRow + ActualGameConstants.MAP_MAX_HEIGHT - 1);
		} else if (mapHeight != null) {
			bottom = Math.min(maxMaxRow, curY + mapHeight - 1);
		} else {
			bottom = Math.min(maxMaxRow, curY + ActualGameConstants.MAP_MAX_HEIGHT - 1);
		}

		if (minCol != null) {
			left = minCol;
		} else if (maxCol != null) {
			// origin is within [0, 500]
			left = Math.max(0, maxCol - ActualGameConstants.MAP_MAX_WIDTH + 1);
		} else if (mapWidth != null) {
			left = Math.max(0, curX - mapWidth + 1);
		} else {
			left = Math.max(0, curX - ActualGameConstants.MAP_MAX_WIDTH + 1);
		}

		int maxMaxCol = 500 + ActualGameConstants.MAP_MAX_WIDTH - 1;
		if (maxCol != null) {
			right = maxCol;
		} else if (minCol != null) {
			// origin is within [0, 500], ergo this can't be higher than 579
			right = Math.min(maxMaxCol, minCol + ActualGameConstants.MAP_MAX_WIDTH - 1);
		} else if (mapWidth != null) {
			right = Math.min(maxMaxCol, curX + mapWidth - 1);
		} else {
			right = Math.min(maxMaxCol, curX + ActualGameConstants.MAP_MAX_WIDTH - 1);
		}

		int maxVert = Math.max(curY - bottom, top - curY);
		int maxHori = Math.max(curX - left, right - curX);
		return maxVert * maxVert + maxHori * maxHori;
	}

}
