package spadi.controller.database

import spadi.controller.Globals._

/**
  * Used for accessing database tables for S-Price project
  * @author Mikko Hilpinen
  * @since 28.7.2020, v1.2
  */
object Tables
{
	// ATTRIBUTES	---------------------------
	
	/**
	  * Name of the database that contains these tables
	  */
	val databaseName = "s_price"
	
	/**
	  * Name of the table that contains database version recordings
	  */
	val versionTableName = "database_version"
	
	private val tables = new utopia.vault.database.Tables(connectionPool)
	
	
	// COMPUTED	-------------------------------
	
	/**
	  * @return Table that contains database version recordings
	  */
	def databaseVersion = apply(versionTableName)
	
	
	// OTHER	-------------------------------
	
	private def apply(tableName: String) = tables(databaseName, tableName)
}
