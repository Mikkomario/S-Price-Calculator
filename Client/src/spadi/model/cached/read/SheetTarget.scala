package spadi.model.cached.read

object SheetTarget
{
	/**
	 * @param sheetName Name of the targeted sheet
	 * @param rangeStart Row + Cell index where the reading is started (Eg. if you wish to skip the first row, pass (1, 0)).
	 *                   Defaults to (0, 0) which means that whole range is read.
	 * @param maxRowsRead Maximum number of rows parsed. None if all rows should be parsed (default).
	 * @param maxCellsRead Maximum number of cells parsed per row. None if all cells should be parsed (default).
	 * @param cellHeadersRowIndex Index of the row that describes cell headers (should be smaller or equal to the first
	 *                   row index read). Default = 0.
	 * @return A target within a sheet with specified name
	 */
	def sheetWithName(sheetName: String, rangeStart: (Int, Int) = (0, 0), maxRowsRead: Option[Int] = None,
					  maxCellsRead: Option[Int] = None, cellHeadersRowIndex: Int = 0) =
		SheetTarget(Left(sheetName), rangeStart, maxRowsRead, maxCellsRead, cellHeadersRowIndex)
	
	/**
	 * @param index Index of the targeted sheet
	 * @param rangeStart Row + Cell index where the reading is started (Eg. if you wish to skip the first row, pass (1, 0)).
	 *                   Defaults to (0, 0) which means that whole range is read.
	 * @param maxRowsRead Maximum number of rows parsed. None if all rows should be parsed (default).
	 * @param maxCellsRead Maximum number of cells parsed per row. None if all cells should be parsed (default).
	 * @param cellHeadersRowIndex Index of the row that describes cell headers (should be smaller or equal to the first
	 *                   row index read). Default = 0.
	 * @return A target within a sheet at specified index
	 */
	def sheetAtIndex(index: Int, rangeStart: (Int, Int) = (0, 0), maxRowsRead: Option[Int] = None,
					 maxCellsRead: Option[Int] = None, cellHeadersRowIndex: Int = 0) =
		SheetTarget(Right(index), rangeStart, maxRowsRead, maxCellsRead, cellHeadersRowIndex)
}

/**
 * Used for specifying a read area (sheet or a part of one) within an excel workbook
 * @author Mikko Hilpinen
 * @since 16.4.2020, v1
 * @param sheetId Either Right: index of the targeted sheet or Left: Name of the targeted sheet
 * @param rangeStart Row + Cell index where the reading is started (Eg. if you wish to skip the first row, pass (1, 0)).
 *                   Defaults to (0, 0) which means that whole range is read.
 * @param maxRowsRead Maximum number of rows parsed. None if all rows should be parsed (default).
 * @param maxCellsRead Maximum number of cells parsed per row. None if all cells should be parsed (default).
 * @param cellHeadersRowIndex Index of the row that describes cell headers (should be smaller or equal to the first
 *                            row index read). Default = 0.
 */
case class SheetTarget(sheetId: Either[String, Int], rangeStart: (Int, Int) = (0, 0), maxRowsRead: Option[Int] = None,
					   maxCellsRead: Option[Int] = None, cellHeadersRowIndex: Int = 0)
{
	/**
	 * @return The index of the first row that is read
	 */
	def firstRowIndex = rangeStart._1
	
	/**
	 * @return The index of the first cell that is read
	 */
	def firstCellIndex = rangeStart._2
}

