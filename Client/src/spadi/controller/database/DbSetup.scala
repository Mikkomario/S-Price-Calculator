package spadi.controller.database

import java.nio.file.Path

import ch.vorburger.mariadb4j.{DB, DBConfigurationBuilder}
import utopia.flow.async.CloseHook
import utopia.flow.util.FileExtensions._
import spadi.controller.Globals._
import utopia.genesis.generic.GenesisDataType

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
		
		// TODO: Set Vault connection path and other parameters
		// See: Connection conn = DriverManager.getConnection(configBuilder.getURL(dbName), "root", "");
		
		database.start()
		
		// Closes the database when program closes
		CloseHook.registerAction { database.stop() }
	}
}
