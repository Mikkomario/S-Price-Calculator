package spadi.controller.read

import java.io.{FileInputStream, FileNotFoundException, IOException}
import java.nio.file.Path
import java.time.LocalTime

import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.openxml4j.opc.OPCPackage
import org.apache.poi.ss.usermodel._
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import spadi.model.cached.read.SheetTarget
import utopia.flow.datastructure.immutable.{Constant, Model, Value}
import utopia.flow.generic.ValueConversions._
import utopia.flow.util.FileExtensions._
import utopia.flow.util.CollectionExtensions._
import utopia.flow.util.AutoClose._

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
	
	// TODO: Methods in this file contain a lot of copy paste because alternative, non-targeted versions were
	//  introduced after previous versions were already written and in use. Remove or refactor additional versions once
	//  the use cases have been narrowed down
	// TODO: To be more precise, there are roughly 3 versions of each method (with sheet target, auto-targeted and foreach)
	
	/**
	  * Reads a single sheet from an excel file
	  * @param path   Path to the excel file
	  * @param target Target that specifies which sheet (or a portion of a sheet) should be read
	  * @return Read sheet's data as a vector of models. Failure if no data could be read. If the sheet didn't exist
	  *         in the excel, returns an empty vector.
	  */
	def from(path: Path, target: SheetTarget): Try[Vector[Model[Constant]]] = from(path, HashMap("a" -> target)).map
	{
		_.getOrElse("a", Vector())
	}
	
	/**
	  * Reads a single sheet from an excel file
	  * @param path   Path to the excel file
	  * @param target Target that specifies which sheet (or a portion of a sheet) should be read
	  * @return Read sheet's data as a vector of value vectors (rows). Failure if no data could be read.
	  *         If the sheet didn't exist in the excel, returns an empty vector.
	  */
	def withoutHeadersFrom(path: Path, target: SheetTarget): Try[Vector[Vector[Value]]] =
		withoutHeadersFrom(path, HashMap("a" -> target)).map
		{_.getOrElse("a", Vector())}
	
	/**
	  * Reads row data as models from specified excel file
	  * @param path    An excel file
	  * @param targets Targets specifying which sheets and areas should be read (each target is matched with a unique key)
	  * @return Read data matching specified target keys. Failure if data couldn't be read
	  */
	def from(path: Path, targets: Map[String, SheetTarget]) =
		readFileWith(path, targets)(parseWorkBook)
	
	/**
	  * Reads row data as value lists from specified excel file
	  * @param path    An excel file path
	  * @param targets Targets specifying which sheets and areas should be read. Each target is matched with a unique key.
	  * @return Read data matching specified target keys. Failure if data couldn't be read
	  */
	def withoutHeadersFrom(path: Path, targets: Map[String, SheetTarget]) =
		readFileWith(path, targets)(onlyParseRows)
	
	/**
	  * Parses models from a worksheet at specified index
	  * @param path Excel file path
	  * @param sheetIndex Index of the targeted sheet (0 means the first sheet)
	  * @param expectedColumnCount Expected (minimum) number of columns in the header row (default = 1)
	  * @return Parsed models from the targeted file
	  */
	def fromSheetAtIndex(path: Path, sheetIndex: Int, expectedColumnCount: Int = 1) =
		readFileWith2(path) { book => parseWorkBook2(book, Right(sheetIndex), expectedColumnCount) }
	
	/**
	  * Parses models from a worksheet with specified name
	  * @param path Excel file path
	  * @param sheetName Name of the targeted worksheet
	  * @param expectedColumnCount Expected (minimum) number of columns in the header row (default = 1)
	  * @return Parsed models from the targeted file
	  */
	def fromSheetWithName(path: Path, sheetName: String, expectedColumnCount: Int = 1) =
		readFileWith2(path) { book => parseWorkBook2(book, Left(sheetName), expectedColumnCount) }
	
	/**
	  * Parses rows from a worksheet at specified index
	  * @param path Excel file path
	  * @param sheetIndex Index of the targeted sheet (0 means the first sheet)
	  * @return Parsed rows from the targeted file
	  */
	def withoutHeadersFromSheetAtIndex(path: Path, sheetIndex: Int) =
		readFileWith2(path) { onlyParseRows2(_, Right(sheetIndex)) }
	
	/**
	  * Parses rows from a worksheet with specified name
	  * @param path Excel file path
	  * @param sheetName Name of the targeted worksheet
	  * @return Parsed rows from the targeted file
	  */
	def withoutHeadersFromSheetWithName(path: Path, sheetName: String) =
		readFileWith2(path) { onlyParseRows2(_, Left(sheetName)) }
	
	/**
	  * Processes each row in the targeted document using specified function
	  * @param path Path to target document
	  * @param sheetIndex Index of the targeted worksheet inside the document where 0 is the first sheet
	  * @param expectedColumnCount Minimum number of columns in the header row (default = 1)
	  * @param processor A function called for each read row
	  * @return Failure if file read failed. Success otherwise.
	  */
	def foreachRowInSheetAtIndex(path: Path, sheetIndex: Int, expectedColumnCount: Int = 1)
								(processor: Model[Constant] => Unit) =
		readFileWith2(path) { book => processWorkBook(book, Right(sheetIndex), expectedColumnCount, processor) }
	
	/**
	  * Processes each row in the targeted document using specified function
	  * @param path Path to target document
	  * @param sheetName Name of the targeted worksheet inside the document
	  * @param expectedColumnCount Minimum number of columns in the header row (default = 1)
	  * @param processor A function called for each read row
	  * @return Failure if file read failed. Success otherwise.
	  */
	def foreachRowInSheetWithName(path: Path, sheetName: String, expectedColumnCount: Int = 1)
								(processor: Model[Constant] => Unit) =
		readFileWith2(path) { book => processWorkBook(book, Left(sheetName), expectedColumnCount, processor) }
	
	private def readFileWith[Result](path: Path, targets: Map[String, SheetTarget])(f: (Workbook, Map[String, SheetTarget]) => Result) =
	{
		if (path.isRegularFile)
		{
			Try
			{
				// Uses either xls (HSSF) or xlsx parsing
				path.fileType match
				{
					case "xls" => new FileInputStream(path.toFile).consume
					{ stream =>
						f(new HSSFWorkbook(stream), targets)
					}
					case _ => Option(OPCPackage.open(path.toFile)).toTry
					{
						new IOException(s"Couldn't open excel file from $path")
					}.get.consume
					{ pkg =>
						f(new XSSFWorkbook(pkg), targets)
					}
				}
			}
		}
		else
			Failure(new FileNotFoundException(s"There is no existing regular file at $path"))
	}
	
	private def readFileWith2[Result](path: Path)(f: Workbook => Result) =
	{
		if (path.isRegularFile)
		{
			Try
			{
				// Uses either xls (HSSF) or xlsx parsing
				path.fileType match
				{
					case "xls" => new FileInputStream(path.toFile).consume { stream => f(new HSSFWorkbook(stream)) }
					case _ => Option(OPCPackage.open(path.toFile)).toTry {
						new IOException(s"Couldn't open excel file from $path")
					}.get.consume { pkg => f(new XSSFWorkbook(pkg)) }
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
				if (idx < numberOfSheets) Some(workBook.getSheetAt(idx))
				else None
			}.map {rowsFromSheet(_, target)}
			
			// Produces map format
			parsedData.map { models => sheetKey -> models }
		}
	}
	
	// NB: May throw
	private def parseWorkBookWith2[Row](workBook: Workbook, target: Either[String, Int])(
		rowsFromSheet: Sheet => Vector[Row]) =
	{
		val numberOfSheets = workBook.getNumberOfSheets
		// Reads targeted sheet
		// Parses row data from the sheet
		target.mapToSingle { name => Option(workBook.getSheet(name)) } { idx =>
			if (idx < numberOfSheets) Some(workBook.getSheetAt(idx))
			else None
		}.map { rowsFromSheet(_) }.getOrElse(Vector())
	}
	
	// NB: May throw
	private def processWorkBookWith(workBook: Workbook, target: Either[String, Int])(processSheet: Sheet => Unit) =
	{
		val numberOfSheets = workBook.getNumberOfSheets
		// Reads targeted sheet
		// Parses row data from the sheet
		target.mapToSingle { name => Option(workBook.getSheet(name)) } { idx =>
			if (idx < numberOfSheets) Some(workBook.getSheetAt(idx))
			else None
		}.foreach { processSheet(_) }
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
				val headers = {
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
	
	// NB: May throw
	private def parseWorkBook2(workBook: Workbook, target: Either[String, Int], expectedColumnCount: Int) =
	{
		parseWorkBookWith2(workBook, target) { sheet =>
			// Reads the cell headers first
			// Skips rows that don't have enough columns to be header rows
			val rowIterator = sheet.rowIterator().asScala.dropWhile { row => row.cellIterator().asScala
				.count { cell => cell.getCellType != CellType.BLANK &&
					(cell.getCellType != CellType.STRING || cell.getStringCellValue.nonEmpty) } <= expectedColumnCount }
			if (rowIterator.hasNext)
			{
				val headersRow = rowIterator.next()
				// Header index -> header name
				val headers = {
					headersRow.cellIterator().asScala.map { cell =>
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
				parseRows2(rowIterator).map { row =>
					val constants = headers.map { case (index, name) =>
						val value = row.getOrElse(index, Value.empty)
						Constant(name, value)
					}
					Model.withConstants(constants)
				}
			}
			else
				Vector()
		}
	}
	
	// NB: May throw
	private def processWorkBook(workBook: Workbook, target: Either[String, Int], expectedColumnCount: Int,
								processor: Model[Constant] => Unit) =
	{
		processWorkBookWith(workBook, target) { sheet =>
			// Reads the cell headers first
			// Skips rows that don't have enough columns to be header rows
			val rowIterator = sheet.rowIterator().asScala.dropWhile { row => row.cellIterator().asScala
				.count { cell => cell.getCellType != CellType.BLANK &&
					(cell.getCellType != CellType.STRING || cell.getStringCellValue.nonEmpty) } <= expectedColumnCount }
			if (rowIterator.hasNext)
			{
				val headersRow = rowIterator.next()
				// Header index -> header name
				val headers = {
					headersRow.cellIterator().asScala.map { cell =>
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
				processRows(rowIterator, headers, processor)
			}
		}
	}
	
	private def onlyParseRows(workbook: Workbook, targets: Map[String, SheetTarget]) =
	{
		parseWorkBookWith(workbook, targets) { (sheet, target) =>
			parseRows(sheet, target.firstRowIndex, target.maxRowsRead, target.firstCellIndex, target.maxCellsRead)
		}
	}
	
	private def onlyParseRows2(workbook: Workbook, target: Either[String, Int]) =
		parseWorkBookWith2(workbook, target) { sheet => parseRows2(sheet.rowIterator().asScala) }
	
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
	
	private def parseRows2(rowsIterator: Iterator[Row]) =
	{
		rowsIterator.map { row =>
			val cellValues = row.cellIterator().asScala.map { cell =>
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
			(0 to lastCellIndex).map { idx => cellValues.getOrElse(idx, Value.empty) }.toVector
		}.toVector
	}
	
	private def processRows(rowsIterator: Iterator[Row], headers: Vector[(Int, String)],
							processor: Model[Constant] => Unit) =
	{
		rowsIterator.foreach { row =>
			val cellValues = row.cellIterator().asScala.map { cell =>
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
			val parsedValues = (0 to lastCellIndex).map { idx => cellValues.getOrElse(idx, Value.empty) }.toVector
			
			// Transforms the row into a model and processes it
			val constants = headers.map { case (index, name) =>
				val value = parsedValues.getOrElse(index, Value.empty)
				Constant(name, value)
			}
			processor(Model.withConstants(constants))
		}
	}
	
	private def limitedIterator(cellIterator: Iterator[Cell], firstCellIndex: Int = 0, maxCellsRead: Option[Int] = None) =
	{
		val base = cellIterator.dropWhile {_.getColumnIndex < firstCellIndex}
		maxCellsRead.map { max => base.takeWhile {_.getColumnIndex < firstCellIndex + max} }.getOrElse(base)
	}
	
	// Only call for string type cells
	private def cellStringValue(cell: Cell) =
	{
		val base = cell.getStringCellValue
		val text = if (base.startsWith("'")) base.drop(1)
		else base
		text.trim
	}
}
