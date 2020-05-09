package spadi.controller

import java.io.{FileInputStream, FileNotFoundException, IOException}
import java.nio.file.Path
import java.time.LocalTime

import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.openxml4j.opc.OPCPackage
import org.apache.poi.ss.usermodel.{CellType, DateUtil, Workbook}
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
	 * Reads a single sheet from an excel file
	 * @param path Path to the excel file
	 * @param target Target that specifies which sheet (or a portion of a sheet) should be read
	 * @return Read sheet's data as a vector of models. Failure if no data could be read. If the sheet didn't exist
	 *         in the excel, returns an empty vector.
	 */
	def from(path: Path, target: SheetTarget): Try[Vector[Model[Constant]]] = from(path, HashMap("a" -> target)).map {
		_.getOrElse("a", Vector()) }
	
	/**
	 * Reads row data as models from specified excel file
	 * @param path An excel file
	 * @param targets Targets specifying which sheets and areas should be read (each target is matched with a unique key)
	 * @return Read data matching specified target keys. Failure if data couldn't be read
	 */
	def from(path: Path, targets: Map[String, SheetTarget]) =
	{
		if (path.isRegularFile)
		{
			Try {
				// Uses either xls (HSSF) or xlsx parsing
				path.fileType match
				{
					case "xls" => new FileInputStream(path.toFile).consume { stream =>
						parseWorkBook(new HSSFWorkbook(stream), targets) }
					case _ => Option(OPCPackage.open(path.toFile)).toTry {
						new IOException(s"Couldn't open excel file from $path") }.get.consume { pkg =>
						parseWorkBook(new XSSFWorkbook(pkg), targets) }
				}
			}
		}
		else
			Failure(new FileNotFoundException(s"There is no existing regular file at $path"))
	}
	
	// NB: May throw
	private def parseWorkBook(workBook: Workbook, targets: Map[String, SheetTarget]) =
	{
		val numberOfSheets = workBook.getNumberOfSheets
		// Reads targeted sheets
		targets.flatMap { case (sheetKey, target) =>
			val parsedModels = target.sheetId.mapToSingle { name => Option(workBook.getSheet(name)) } { idx =>
				if (idx < numberOfSheets) Some(workBook.getSheetAt(idx)) else None }.flatMap { sheet =>
				
				// Reads the cell headers first
				val findHeadersIterator = sheet.rowIterator().asScala.drop(target.cellHeadersRowIndex)
				if (findHeadersIterator.hasNext)
				{
					val headersRow = findHeadersIterator.next()
					val headers =
					{
						val base = headersRow.cellIterator().asScala.drop(target.firstCellIndex)
						target.maxCellsRead.map { max => base.take(max) }.getOrElse(base).toVector.mapWithIndex { (cell, idx) =>
							cell.getCellType match
							{
								case CellType.BOOLEAN => cell.getBooleanCellValue.toString
								case CellType.NUMERIC => cell.getNumericCellValue.toString
								case CellType.STRING => cell.getStringCellValue
								case _ => idx.toString
							}
						}
					}
					
					// Then parses targeted rows to models
					val rowsIterator =
					{
						val base = sheet.rowIterator().asScala.drop(target.firstRowIndex)
						val afterHeader =
						{
							if (target.cellHeadersRowIndex >= target.firstRowIndex)
							{
								val skippedFromBeginning = target.cellHeadersRowIndex - target.firstRowIndex + 1
								base.drop(skippedFromBeginning)
							}
							else
								base
						}
						target.maxRowsRead.map { max => afterHeader.take(max) }.getOrElse(afterHeader)
					}
					val models = rowsIterator.map { row =>
						val cellsIterator =
						{
							val base = row.cellIterator().asScala.drop(target.firstCellIndex)
							target.maxCellsRead.map { max => base.take(max) }.getOrElse(base)
						}
						val properties = cellsIterator.zip(headers).map { case (cell, header) =>
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
										cell.getNumericCellValue
								case CellType.STRING => cell.getStringCellValue
								case _ => Value.empty
							}
							header -> value
						}
						Model(properties.toVector)
					}
					Some(models.toVector)
				}
				else
					None
			}
			
			// Produces map format
			parsedModels.map { models => sheetKey -> models }
		}
	}
}