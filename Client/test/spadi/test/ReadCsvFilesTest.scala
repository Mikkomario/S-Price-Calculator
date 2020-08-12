package spadi.test

import spadi.controller.Globals
import spadi.controller.database.DbSetup
import spadi.controller.database.access.multi.DbPriceKeyMappings
import spadi.model.cached.ProgressState
import spadi.view.util.Setup
import utopia.flow.datastructure.mutable.PointerWithEvents
import utopia.flow.parse.CsvReader
import utopia.flow.util.FileExtensions._
import utopia.flow.util.CollectionExtensions._

import scala.io.Codec

/**
  * Tests csv file reading
  * @author Mikko Hilpinen
  * @since 12.8.2020, v1.2
  */
object ReadCsvFilesTest extends App
{
	private implicit val codec: Codec = Codec.UTF8
	private implicit val languageCode: String = "en"
	
	import Setup._
	
	val progressPointer = new PointerWithEvents(ProgressState.initial("Setting up DB"))
	progressPointer.addListener { e => println(e.newValue) }
	DbSetup.setup()
	
	val mappings = connectionPool { implicit c => DbPriceKeyMappings.all }
	
	Globals.fileInputDirectory.allRegularFileChildrenOfType("csv").get.foreach { p =>
		println(s"\nListing first items from ${p.fileName}")
		val lines = CsvReader.iterateLinesIn(p) { lines => lines.take(10).toVector }.get
		lines.foreach { println(_) }
		println()
		mappings.foreach { mapping =>
			lines.tryMap { mapping(_) }.foreach { parsed =>
				println(s"Using mapping: $mapping")
				println(s"Mapping price key: '${mapping.priceKey}'")
				val price = lines.head(mapping.priceKey)
				println(s"Read price: ${price.description} => ${price.getDouble}")
				parsed.foreach { println(_) }
			}
		}
	}
	
	println("\nDone")
	System.exit(0)
}
