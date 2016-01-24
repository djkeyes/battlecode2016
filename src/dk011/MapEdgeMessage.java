package dk011;

/**
 * A message representing information about the min/max row/column, or the map
 * height/width. In fact, since each of those quantities are pretty small, and
 * doing a large broadcast can be expensive, this actually combines all of those
 * into a single message.
 */
public class MapEdgeMessage extends Message {

	@Override
	protected long getMessageOrdinal() {
		return MAP_EDGE_MESSAGE;
	}

	public Integer minRow, maxRow, minCol, maxCol;
	public Integer mapWidth, mapHeight;

	// bit of a misnomer, this is the max origin value (500), plus one
	private static final int MAX_ORIGIN_VALUE = 501;
	private static final int MAX_ANTI_ORIGIN_VALUE = 581;

	/******** SENDING *********/

	public MapEdgeMessage(Integer minRow, Integer minCol, Integer maxRow, Integer maxCol, Integer mapWidth,
			Integer mapHeight) {
		this.minRow = minRow;
		this.minCol = minCol;
		this.maxRow = maxRow;
		this.maxCol = maxCol;
		this.mapWidth = mapWidth;
		this.mapHeight = mapHeight;
	}

	public int[] encodeMessage() {
		appendData(minRow != null ? 1 : 0, 2);
		appendData(minRow != null ? minRow : 0, MAX_ORIGIN_VALUE);
		appendData(minCol != null ? 1 : 0, 2);
		appendData(minCol != null ? minCol : 0, MAX_ORIGIN_VALUE);
		// to be honest, we could compress these into the range [0,500] by
		// subtracting 80, but it turns out we have enough bits regardless
		appendData(maxRow != null ? 1 : 0, 2);
		appendData(maxRow != null ? maxRow : 0, MAX_ANTI_ORIGIN_VALUE);
		appendData(maxCol != null ? 1 : 0, 2);
		appendData(maxCol != null ? maxCol : 0, MAX_ANTI_ORIGIN_VALUE);
		appendData(mapWidth != null ? 1 : 0, 2);
		appendData(mapWidth != null ? mapWidth : 0, ActualGameConstants.MAP_MAX_WIDTH + 1);
		appendData(mapHeight != null ? 1 : 0, 2);
		appendData(mapHeight != null ? mapHeight : 0, ActualGameConstants.MAP_MAX_HEIGHT + 1);

		return super.encodeMessage();
	}

	/******* RECEIVING ********/

	public static void processMessage(long bits) {
		// this directly modifies variables in MapEdgesReceiver.
		// we could wait and store this message in some kind of queue, but we
		// might as well use the information as soon as possible.

		setBits(bits);

		int tmp;
		tmp = (int) consumeData(ActualGameConstants.MAP_MAX_HEIGHT + 1);
		if (consumeData(2) > 0) {
			MapEdgesReceiver.mapHeight = tmp;
		}
		tmp = (int) consumeData(ActualGameConstants.MAP_MAX_WIDTH + 1);
		if (consumeData(2) > 0) {
			MapEdgesReceiver.mapWidth = tmp;
		}

		tmp = (int) consumeData(MAX_ANTI_ORIGIN_VALUE);
		if (consumeData(2) > 0) {
			MapEdgesReceiver.maxCol = tmp;
		}
		tmp = (int) consumeData(MAX_ANTI_ORIGIN_VALUE);
		if (consumeData(2) > 0) {
			MapEdgesReceiver.maxRow = tmp;
		}

		tmp = (int) consumeData(MAX_ORIGIN_VALUE);
		if (consumeData(2) > 0) {
			MapEdgesReceiver.minCol = tmp;
		}
		tmp = (int) consumeData(MAX_ORIGIN_VALUE);
		if (consumeData(2) > 0) {
			MapEdgesReceiver.minRow = tmp;
		}
	}

}
