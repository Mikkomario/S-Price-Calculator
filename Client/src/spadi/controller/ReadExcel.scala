package spadi.controller

import java.io.{FileInputStream, FileNotFoundException, IOException}
import java.nio.file.Path
import java.time.LocalTime

import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.openxml4j.opc.OPCPackage
import org.apache.poi.ss.usermodel.{Cell, CellType, DateUtil, Sheet, Workbook}
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import utopia.flow.datastructure.immutable.{Constant, Model, Value}
import utopia.flow.util.FileExtensions._
import utopia.flow.util.AutoClose._
import utopia.flow.util.CollectionExtensions._
import utopia.flow.generic.ValueConversions._

import scala.collection.immutable.HashMap
import scala.util.{Failure, Try}
import scala.jdk.CollectionConverters._

/**
 * Used for processing excel (.xls and .xlsx) files
 * @author Mikko Hilpinen
 * @since 16.4.2020, v10.2
 */
object ReadExcel
{
	/**
	 * File types this object is able to read
	 */
	val supportedFileTypes = Vector("xlsx", "xls")
	
	/**
	 * Reads a single sheet from an excel file
	 * @param path Path to the excel file
	 * @param target Target that specifies which sheet (or a portion of a sheet) should be read
	 * @return Read sheet's data as a vector of models. Failure if no data could be read. If the sheet didn't exist
	 *         in the excel, returns an empty vector.
	 */
	def from(path: Path, target: SheetTarget): Try[Vector[Model[Constant]]] = from(path, HashMap("a" -> target)).map {
		_.getOrElse("a", Vector()) }
	
	/**
	 * Reads a single sheet from an excel file
	 * @param path Path to the excel file
	 * @param target Target that specifies which sheet (or a portion of a sheet) should be read
	 * @return Read sheet's data as a vector of value vectors (rows). Failure if no data could be read.
	 *         If the sheet didn't exist in the excel, returns an empty vector.
	 */
	def withoutHeadersFrom(path: Path, target: SheetTarget): Try[Vector[Vector[Value]]] =
		withoutHeadersFrom(path, HashMap("a" -> target)).map { _.getOrElse("a", Vector()) }
	
	/**
	 * Reads row data as models from specified excel file
	 * @param path An excel file
	 * @param targets Targets specifying which sheets and areas should be read (each target is matched with a unique key)
	 * @return Read data matching specified target keys. Failure if data couldn't be read
	 */
	def from(path: Path, targets: Map[String, SheetTarget]) =
		readFileWith(path, targets)(parseWorkBook)
	
	/**
	 * Reads row data as value lists from specified excel file
	 * @param path An excel file path
	 * @param targets Targets specifying which sheets and areas should be read. Each target is matched with a unique key.
	 * @return Read data matching specified target keys. Failure if data couldn't be read
	 */
	def withoutHeadersFrom(path: Path, targets: Map[String, SheetTarget]) =
		readFileWith(path, targets)(onlyParseRows)
	
	private def readFileWith[Result](path: Path, targets: Map[String, SheetTarget])(f: (Workbook, Map[String, SheetTarget]) => Result) =
	{
		if (path.isRegularFile)
		{
			Try {
				// Uses either xls (HSSF) or xlsx parsing
				path.fileType match
				{
					case "xls" => new FileInputStream(path.toFile).consume { stream =>
						f(new HSSFWorkbook(stream), targets) }
					case _ => Option(OPCPackage.open(path.toFile)).toTry {
						new IOException(s"Couldn't open excel file from $path") }.get.consume { pkg =>
						f(new XSSFWorkbook(pkg), targets) }
				}
			}
		}
		else
			Failure(new FileNotFoundException(s"There is no existing regular file at $path"))
	}
	
	// NB: May throw
	private def parseWorkBookWith[Row](workBook: Workbook, targets: Map[String, SheetTarget])(
		rowsFromSheet: (Sheet, SheetTarget) => Vector[Row]) =
	{
		val numberOfSheets = workBook.getNumberOfSheets
		// Reads targeted sheets
		targets.flatMap { case (sheetKey, target) =>
			// Parses row data from the sheets
			val parsedData = target.sheetId.mapToSingle { name => Option(workBook.getSheet(name)) } { idx =>
				if (idx < numberOfSheets) Some(workBook.getSheetAt(idx)) else None }.map { rowsFromSheet(_, target) }
			
			// Produces map format
			parsedData.map { models => sheetKey -> models }
		}
	}
	
	// NB: May throw
	private def parseWorkBook(workBook: Workbook, targets: Map[String, SheetTarget]) =
	{
		parseWorkBookWith(workBook, targets) { (sheet, target) =>
			// Reads the cell headers first
			val findHeadersIterator = sheet.rowIterator().asScala.drop(target.cellHeadersRowIndex)
			if (findHeadersIterator.hasNext)
			{
				val headersRow = findHeadersIterator.next()
				// Header index -> header name
				val headers =
				{
					limitedIterator(headersRow.cellIterator().asScala, target.firstCellIndex, target.maxCellsRead).map { cell =>
						val headerName = cell.getCellType match
						{
							case CellType.BOOLEAN => cell.getBooleanCellValue.toString
							case CellType.NUMERIC => cell.getNumericCellValue.toString
							case CellType.STRING => cellStringValue(cell)
							case _ => cell.getColumnIndex.toString
						}
						cell.getColumnIndex -> headerName
					}
				}.toVector
				
				// Then parses targeted rows to models
				val firstRowIndex = target.firstRowIndex max (target.cellHeadersRowIndex + 1)
				parseRows(sheet, firstRowIndex, target.maxRowsRead, target.firstCellIndex, target.maxCellsRead)
					.map { row =>
						val constants = headers.map { case (index, name) =>
							val value = row.getOrElse(index - target.firstCellIndex, Value.empty)
							Constant(name, value)
						}
						Model.withConstants(constants)
					}
			}
			else
				Vector()
		}
	}
	
	private def onlyParseRows(workbook: Workbook, targets: Map[String, SheetTarget]) =
	{
		parseWorkBookWith(workbook, targets) { (sheet, target) =>
			parseRows(sheet, target.firstRowIndex, target.maxRowsRead, target.firstCellIndex, target.maxCellsRead)
		}
	}
	
	private def parseRows(sheet: Sheet, firstRowIndex: Int = 0, maxRowsRead: Option[Int] = None, firstCellIndex: Int = 0,
	                      maxCellsRead: Option[Int] = None) =
	{
		// Then parses targeted rows to models
		val rowsIterator =
		{
			val base = sheet.rowIterator().asScala.drop(firstRowIndex)
			maxRowsRead.map { max => base.take(max) }.getOrElse(base)
		}
		rowsIterator.map { row =>
			val cellValues = limitedIterator(row.cellIterator().asScala, firstCellIndex, maxCellsRead).map { cell =>
				val value: Value = cell.getCellType match
				{
					case CellType.BOOLEAN => cell.getBooleanCellValue
					case CellType.FORMULA => cell.getCellFormula
					case CellType.NUMERIC =>
						// Number may also represent a date (with or without time)
						if (DateUtil.isCellDateFormatted(cell))
						{
							val date = cell.getLocalDateTimeCellValue
							if (date.toLocalTime.equals(LocalTime.MIDNIGHT))
								date.toLocalDate
							else
								date
						}
						else
						{
							// Formats the number
							val doubleNumber = cell.getNumericCellValue
							if (doubleNumber % 1 == 0)
							{
								if (doubleNumber > Int.MaxValue)
									doubleNumber.toLong
								else
									doubleNumber.toInt
							}
							else
								doubleNumber
						}
					case CellType.STRING => cellStringValue(cell)
					case _ => Value.empty
				}
				cell.getColumnIndex -> value
			}.toMap
			// Fills missing cell values as empty values
			val lastCellIndex = cellValues.keys.max
			(firstCellIndex to lastCellIndex).map { idx => cellValues.getOrElse(idx, Value.empty) }.toVector
		}.toVector
	}
	
	private def limitedIterator(cellIterator: Iterator[Cell], firstCellIndex: Int = 0, maxCellsRead: Option[Int] = None) =
	{
		val base = cellIterator.dropWhile { _.getColumnIndex < firstCellIndex }
		maxCellsRead.map { max => base.takeWhile { _.getColumnIndex < firstCellIndex + max } }.getOrElse(base)
	}
	
	// Only call for string type cells
	private def cellStringValue(cell: Cell) =
	{
		val base = cell.getStringCellValue
		val text = if (base.startsWith("'")) base.drop(1) else base
		text.trim
	}
}