package spadi.controller

import java.io.{PrintWriter, StringWriter}
import java.nio.file.Path
import java.time.{LocalDate, LocalTime}

import utopia.flow.util.FileExtensions._
import utopia.flow.util.AutoClose._
import utopia.flow.util.CollectionExtensions._

import scala.io.Codec

/**
 * Used for logging errors
 * @author Mikko Hilpinen
 * @since 8.5.2020, v1
 */
object Log
{
	// ATTRIBUTES	------------------------------
	
	private implicit val codec: Codec = Codec.UTF8
	
	
	// OTHER	----------------------------------
	
	/**
	  * Writes a new error to this log
	  * @param error Error to log
	  * @param message Error message
	  */
	def apply(error: Throwable, message: String): Unit =
	{
		// Prints to console
		println(message)
		error.printStackTrace()
		
		// Logs to an error file
		val stackStringWriter = new StringWriter()
		new PrintWriter(stackStringWriter).consume(error.printStackTrace)
		
		val targetPath: Path = s"log/log-${LocalDate.now()}.txt"
		targetPath.createParentDirectories()
			.flatMap { _.append(s"\n${LocalTime.now()}: $message\n${stackStringWriter.toString}") }
			.failure.foreach { e =>
				println("Failed to log error to file")
				e.printStackTrace()
			}
	}
}
