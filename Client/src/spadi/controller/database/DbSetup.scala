package spadi.controller.database

import java.nio.file.Path

import ch.vorburger.mariadb4j.{DB, DBConfigurationBuilder}
import utopia.flow.async.CloseHook
import utopia.flow.util.FileExtensions._
import spadi.controller.Globals._
import spadi.controller.database.access.single.DbDatabaseVersion
import utopia.genesis.generic.GenesisDataType
import utopia.vault.database.Connection

/**
  * Used for setting up a local database
  * @author Mikko Hilpinen
  * @since 28.7.2020, v1.2
  */
object DbSetup
{
	// See: https://github.com/vorburger/MariaDB4j
	
	def setup() =
	{
		GenesisDataType.setup()
		
		// Sets up the database
		val configBuilder = DBConfigurationBuilder.newBuilder()
		configBuilder.setPort(0)
		configBuilder.setDataDir(("database": Path).absolute.toString)
		val database = DB.newEmbeddedDB(configBuilder.build())
		
		// Updates Vault connection settings
		Connection.modifySettings { _.copy(connectionTarget = configBuilder.getURL("")) }
		
		// Starts the database
		// TODO: Handle thrown exceptions
		database.start()
		// Closes the database when program closes
		CloseHook.registerAction { database.stop() }
		
		// Checks current database version, and whether database has been configured at all
		val currentDbVersion = connectionPool.tryWith { implicit connection =>
			if (connection.existsTable(Tables.databaseName, Tables.versionTableName))
				DbDatabaseVersion.latest
			else
				None
		}
		
		// Performs a database structure update if necessary
		// TODO: Update DB structure if needed
		
		// TODO: Import database structure on first time and version updates
		// db.source("path/to/resource.sql");
	}
}
