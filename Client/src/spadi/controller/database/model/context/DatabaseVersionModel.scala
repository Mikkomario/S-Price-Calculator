package spadi.controller.database.model.context

import java.time.Instant

import spadi.controller.database.factory.context.DatabaseVersionFactory
import spadi.model.cached.VersionNumber
import spadi.model.partial.DatabaseVersionData
import spadi.model.stored.DatabaseVersion
import utopia.flow.generic.ValueConversions._
import utopia.vault.database.Connection
import utopia.vault.model.immutable.StorableWithFactory

object DatabaseVersionModel
{
	/**
	  * Inserts a new database version to the database
	  * @param data Data to insert
	  * @param connection DB Connection (implicit)
	  * @return Newly inserted version
	  */
	def insert(data: DatabaseVersionData)(implicit connection: Connection) =
	{
		val id = apply(None, Some(data.number), Some(data.created)).insert().getInt
		DatabaseVersion(id, data)
	}
}

/**
  * Used for interacting with database version data in DB
  * @author Mikko Hilpinen
  * @since 28.7.2020, v1.2
  */
case class DatabaseVersionModel(id: Option[Int] = None, number: Option[VersionNumber] = None,
								created: Option[Instant] = None) extends StorableWithFactory[DatabaseVersion]
{
	override def factory = DatabaseVersionFactory
	
	override def valueProperties = Vector("id" -> id, "version" -> number.map { _.toString }, "created" -> created)
}
