package spadi.test

import java.time.Instant

import spadi.controller.container.Prices
import utopia.bunnymunch.jawn.JsonBunny
import utopia.flow.generic.DataType
import utopia.flow.parse.JSONReader
import utopia.flow.util.TimeExtensions._

/**
 * Tests speed of alternative json parsers
 * @author Mikko Hilpinen
 * @since 12.5.2020, v1
 */
object JsonParseTest extends App
{
	DataType.setup()
	
	val sourcePath = Prices.fileLocation
	
	val startTime = Instant.now()
	val bunnyJson = JsonBunny(sourcePath)
	val middleTime = Instant.now()
	val readerJson = JSONReader(sourcePath)
	val endTime = Instant.now()
	
	println(s"Bunny: ${(middleTime - startTime).description}")
	println(s"Reader: ${(endTime - middleTime).description}")
}
