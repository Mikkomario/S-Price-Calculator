package spadi.controller.database.access.multi

import spadi.controller.database.factory.context.DatabaseVersionFactory
import spadi.controller.database.model.context.DatabaseVersionModel
import spadi.model.cached.VersionNumber
import spadi.model.partial.DatabaseVersionData
import spadi.model.stored.DatabaseVersion
import utopia.vault.database.Connection
import utopia.vault.nosql.access.ManyRowModelAccess

/**
  * Used for accessing multiple recorded database versions at once
  * @author Mikko Hilpinen
  * @since 28.7.2020, v1.2
  */
object DbDatabaseVersions extends ManyRowModelAccess[DatabaseVersion]
{
	// IMPLEMENTED	-----------------------
	
	override def factory = DatabaseVersionFactory
	
	override def globalCondition = None
	
	
	// COMPUTED	---------------------------
	
	private def model = DatabaseVersionModel
	
	
	// OTHER	---------------------------
	
	/**
	  * Inserts a new database version to the DB
	  * @param versionNumber New version number
	  * @param connection DB Connection (implicit)
	  * @return Newly inserted version model
	  */
	def insert(versionNumber: VersionNumber)(implicit connection: Connection) =
		model.insert(DatabaseVersionData(versionNumber))
}
