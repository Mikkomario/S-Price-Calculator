package spadi.controller

import java.nio.file.Path

import utopia.flow.async.ThreadPool
import utopia.flow.util.FileExtensions._

import scala.concurrent.ExecutionContext

/**
 * Contains global contextual values used throughout the project
 * @author Mikko Hilpinen
 * @since 8.5.2020, v1
 */
object Globals
{
	/**
	 * Asynchronous execution context used in this project
	 */
	implicit val executionContext: ExecutionContext = new ThreadPool("S-Padi").executionContext
	
	val dataDirectory: Path = "data"
}
