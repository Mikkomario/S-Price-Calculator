package spadi.test

import java.nio.file.Path

import spadi.controller.read.ReadExcel
import spadi.model.cached.read.SheetTarget
import utopia.flow.generic.DataType
import utopia.flow.util.FileExtensions._

import scala.util.{Failure, Success}

/**
 * Tests excel file reading & interpretation
 * @author Mikko Hilpinen
 * @since 8.5.2020, v1
 */
object ExcelReadTest extends App
{
	DataType.setup()
	
	val filesDirectory: Path = "Client/test-data"
	val productsFile = filesDirectory/"products.xlsx"
	val pricesFile = filesDirectory/"sales.xlsx"
	
	ReadExcel.from(productsFile, SheetTarget.sheetAtIndex(0, 3 -> 0, Some(5), cellHeadersRowIndex = 2)) match
	{
		case Success(models) =>
			println(s"Read ${models.size} models from products:")
			models.foreach { m => println(s"- $m") }
		case Failure(error) => error.printStackTrace()
	}
	
	ReadExcel.from(pricesFile, SheetTarget.sheetAtIndex(0, 2 -> 0, Some(5), cellHeadersRowIndex = 1)) match
	{
		case Success(models) =>
			println(s"Read ${models.size} models from prices:")
			models.foreach { m => println(s"- $m") }
		case Failure(error) => error.printStackTrace()
	}
}
