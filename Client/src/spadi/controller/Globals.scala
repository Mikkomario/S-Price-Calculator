package spadi.controller

import java.nio.file.Path

import utopia.flow.async.ThreadPool
import utopia.flow.util.FileExtensions._
import utopia.vault.database.ConnectionPool

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
	
	/**
	  * Directory under which local files are stored
	  */
	val dataDirectory: Path = "data"
	
	/**
	  * A directory that contains the files to import
	  */
	val fileInputDirectory: Path = "luettavat-tiedostot"
	
	/**
	  * A directory that contains the files that have already been imported
	  */
	val fileHistoryDirectory: Path = "luetut-tiedostot"
	
	/**
	  * Directory under which database structure changes should be added
	  */
	val sqlImportDirectory: Path = "sql"
	
	/**
	  * Connection pool used when creating database connections
	  */
	implicit val connectionPool: ConnectionPool = new ConnectionPool()
}
