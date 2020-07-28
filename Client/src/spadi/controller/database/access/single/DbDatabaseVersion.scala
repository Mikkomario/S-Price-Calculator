package spadi.controller.database.access.single

import spadi.controller.database.factory.context.DatabaseVersionFactory
import spadi.model.stored.DatabaseVersion
import utopia.vault.database.Connection
import utopia.vault.nosql.access.SingleRowModelAccess

/**
  * Used for accessing individual database versions from the DB
  * @author Mikko Hilpinen
  * @since 28.7.2020, v1.2
  */
object DbDatabaseVersion extends SingleRowModelAccess[DatabaseVersion]
{
	// IMPLEMENTED	-----------------------
	
	override def factory = DatabaseVersionFactory
	
	override def globalCondition = None
	
	
	// COMPUTED	---------------------------
	
	/**
	  * @param connection Database connection (implicit)
	  * @return Latest database version recording. None if no versions have been recorded yet.
	  */
	def latest(implicit connection: Connection) = factory.latest
}
