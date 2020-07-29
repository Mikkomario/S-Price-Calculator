package spadi.controller.database

import java.io.FileNotFoundException
import java.nio.file.Path

import ch.vorburger.mariadb4j.{DB, DBConfigurationBuilder}
import utopia.flow.async.CloseHook
import utopia.flow.util.FileExtensions._
import utopia.flow.util.CollectionExtensions._
import spadi.controller.Globals._
import spadi.controller.database.access.multi.DbDatabaseVersions
import spadi.controller.database.access.single.DbDatabaseVersion
import spadi.model.enumeration.SqlFileType.Full
import utopia.genesis.generic.GenesisDataType
import utopia.vault.database.Connection

import scala.util.{Success, Try}

/**
  * Used for setting up a local database
  * @author Mikko Hilpinen
  * @since 28.7.2020, v1.2
  */
object DbSetup
{
	// See: https://github.com/vorburger/MariaDB4j
	
	/**
	  * Sets up database access and updates the database to the latest version
	  * @return Current database version on success. Failure if database couldn't be started, created or updated.
	  */
	def setup() =
	{
		GenesisDataType.setup()
		
		Try
		{
			// Sets up the database
			val configBuilder = DBConfigurationBuilder.newBuilder()
			configBuilder.setPort(0)
			configBuilder.setDataDir(("database": Path).absolute.toString)
			val database = DB.newEmbeddedDB(configBuilder.build()) // May throw
			
			// Updates Vault connection settings
			Connection.modifySettings { _.copy(connectionTarget = configBuilder.getURL("")) }
			
			// Starts the database (may throw)
			database.start()
			// Closes the database when program closes
			CloseHook.registerAction { database.stop() }
			
			database
		}.flatMap { database =>
			// Checks current database version, and whether database has been configured at all
			connectionPool.tryWith { implicit connection =>
				if (connection.existsTable(Tables.databaseName, Tables.versionTableName))
					DbDatabaseVersion.latest
				else
					None
			}.flatMap { currentDbVersion =>
				ScanSourceFiles(currentDbVersion.map { _.number }).flatMap { sources =>
					// Fails if database can't be set up
					if (sources.isEmpty)
						currentDbVersion.toTry { new FileNotFoundException(
							"Can't find proper source files for setting up the local database") }
					else
					{
						// Drops the previous database if necessary
						val dropResult = {
							if (currentDbVersion.isDefined && sources.exists {_.fileType == Full})
								connectionPool.tryWith {_.dropDatabase(Tables.databaseName)}
							else
								Success(())
						}
						
						// Imports the source files in order
						dropResult.flatMap { _ =>
							sources.tryForEach { s => Try { database.source(s.path.absolute.toString) } }
						}.flatMap { _ =>
							// Records new database version
							connectionPool.tryWith { implicit connection =>
								DbDatabaseVersions.insert(sources.last.targetVersion)
							}
						}
					}
				}
			}
		}
	}
}
