package spadi.controller.database

import java.io.FileNotFoundException
import java.nio.file.Path

import ch.vorburger.mariadb4j.{DB, DBConfigurationBuilder}
import utopia.flow.async.CloseHook
import utopia.flow.util.FileExtensions._
import utopia.flow.util.CollectionExtensions._
import spadi.view.util.Setup._
import spadi.controller.database.access.multi.DbDatabaseVersions
import spadi.controller.database.access.single.DbDatabaseVersion
import spadi.model.cached.ProgressState
import spadi.model.enumeration.SqlFileType.Full
import utopia.flow.datastructure.mutable.PointerWithEvents
import utopia.genesis.generic.GenesisDataType
import utopia.vault.database.Connection

import scala.util.Try

/**
  * Used for setting up a local database
  * @author Mikko Hilpinen
  * @since 28.7.2020, v1.2
  */
object DbSetup
{
	private implicit val languageCode: String = "fi"
	
	// See: https://github.com/vorburger/MariaDB4j
	
	/**
	  * Sets up database access and updates the database to the latest version
	  * @return Current database version on success. Failure if database couldn't be started, created or updated.
	  */
	// TODO: Remove this version
	def setup() =
	{
		GenesisDataType.setup()
		
		Try
		{
			println("Creating database")
			
			// Sets up the database
			val configBuilder = DBConfigurationBuilder.newBuilder()
			configBuilder.setPort(0)
			configBuilder.setDataDir(("database": Path).absolute.toString)
			val database = DB.newEmbeddedDB(configBuilder.build()) // May throw
			
			// Updates Vault connection settings
			Connection.modifySettings { _.copy(
				connectionTarget = configBuilder.getURL(""),
				defaultDBName = Some("test")) }
			
			println("Starting database")
			
			// Starts the database (may throw)
			database.start()
			// Closes the database when program closes
			CloseHook.registerAction {
				println("Stopping database")
				database.stop()
			}
			
		}.flatMap { _ =>
			// Checks current database version, and whether database has been configured at all
			connectionPool.tryWith { implicit connection =>
				println("Checking current DB version")
				if (connection.existsTable(Tables.databaseName, Tables.versionTableName))
					DbDatabaseVersion.latest
				else
					None
			}.flatMap { currentDbVersion =>
				println(s"DB version before update: ${currentDbVersion.map { _.number.toString }.getOrElse("No database")}")
				ScanSourceFiles(currentDbVersion.map { _.number }).flatMap { sources =>
					println(s"Found ${sources.size} sources: ${sources.mkString(", ")}")
					// Fails if database can't be set up
					if (sources.isEmpty)
						currentDbVersion.toTry { new FileNotFoundException(
							"Can't find proper source files for setting up the local database") }
					else
					{
						connectionPool.tryWith { implicit connection =>
							// Drops the previous database if necessary
							if (currentDbVersion.isDefined && sources.exists {_.fileType == Full})
								connection.dropDatabase(Tables.databaseName)
							
							println("Executing source updates")
							// Imports the source files in order
							sources.foreach { s => connection.executeStatementsFrom(s.path).get }
							
							// Records new database version
							println("Recording new database version")
							DbDatabaseVersions.insert(sources.last.targetVersion)
						}
					}
				}
			}
		}
	}
	
	/**
	  * Clears existing data from the database
	  * @return Success or failure
	  */
	def clear() =  Try {
		// Sets up the database
		val configBuilder = DBConfigurationBuilder.newBuilder()
		configBuilder.setPort(0)
		configBuilder.setDataDir(("database": Path).absolute.toString)
		val database = DB.newEmbeddedDB(configBuilder.build()) // May throw
		
		// Updates Vault connection settings
		Connection.modifySettings { _.copy(
			connectionTarget = configBuilder.getURL(""),
			defaultDBName = Some("test")) }
		
		// Starts the database (may throw)
		database.start()
		// Closes the database when program closes
		CloseHook.registerAction {
			println("Stopping database")
			database.stop()
		}
		
		// Clears old data
		connectionPool { _.dropDatabase(Tables.databaseName) }
	}
	
	/**
	  * Sets up database access and updates the database to the latest version
	  * @return Current database version on success. Failure if database couldn't be started, created or updated.
	  */
	def setup(progressPointer: PointerWithEvents[ProgressState]) =
	{
		val result = Try {
			// Sets up the database
			val configBuilder = DBConfigurationBuilder.newBuilder()
			configBuilder.setPort(0)
			configBuilder.setDataDir(("database": Path).absolute.toString)
			val database = DB.newEmbeddedDB(configBuilder.build()) // May throw
			
			// Updates Vault connection settings
			Connection.modifySettings { _.copy(
				connectionTarget = configBuilder.getURL(""),
				defaultDBName = Some("test")) }
			
			progressPointer.value = ProgressState(0.6, "Käynnistetään tietokantaa")
			
			// Starts the database (may throw)
			database.start()
			// Closes the database when program closes
			CloseHook.registerAction {
				println("Stopping database")
				database.stop()
			}
			
			progressPointer.value = ProgressState(0.8, "Tarkistetaan päivityksiä")
			
		}.flatMap { _ =>
			// Checks current database version, and whether database has been configured at all
			connectionPool.tryWith { implicit connection =>
				if (connection.existsTable(Tables.databaseName, Tables.versionTableName))
					DbDatabaseVersion.latest
				else
					None
			}.flatMap { currentDbVersion =>
				println(s"Version before update: ${currentDbVersion.map { _.toString }.getOrElse("No Database") }")
				ScanSourceFiles(currentDbVersion.map { _.number }).flatMap { sources =>
					// Fails if database can't be set up
					if (sources.isEmpty)
					{
						progressPointer.value = ProgressState.finished("Ei uusia päivityksiä")
						currentDbVersion.toTry { new FileNotFoundException(
							"Can't find proper source files for setting up the local database") }
					}
					else
					{
						println(s"Updating with sources: $sources")
						progressPointer.value = ProgressState(0.9, s"Löydettiin ${sources.size} päivitystä")
						connectionPool.tryWith { implicit connection =>
							// Drops the previous database if necessary
							if (currentDbVersion.isDefined && sources.exists {_.fileType == Full})
							{
								println("Dropping previous database version")
								connection.dropDatabase(Tables.databaseName)
							}
							
							// Imports the source files in order
							val progressPerSource = 0.1 / sources.size
							sources.foreachWithIndex { (s, index) =>
								progressPointer.value = ProgressState(0.9 + index * progressPerSource,
									s"Päivitetään versioon ${s.targetVersion} (${index + 1}/${sources.size})")
								connection.executeStatementsFrom(s.path).get
							}
							
							// Records new database version
							DbDatabaseVersions.insert(sources.last.targetVersion)
						}
					}
				}
			}
		}
		
		progressPointer.value = ProgressState.finished(
			if (result.isSuccess) "Tietokanta valmiina käyttöön" else "Tietokannan alustus epäonnistui")
		
		result
	}
}
