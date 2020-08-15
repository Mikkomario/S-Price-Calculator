package spadi.controller.database.factory.context

import spadi.controller.database.Tables
import spadi.model.cached.VersionNumber
import spadi.model.partial.DatabaseVersionData
import spadi.model.stored.DatabaseVersion
import utopia.flow.datastructure.immutable.{Constant, Model}
import utopia.flow.generic.ValueUnwraps._
import utopia.vault.nosql.factory.{FromRowFactoryWithTimestamps, FromValidatedRowModelFactory}

/**
  * Used for reading database version data from DB
  * @author Mikko Hilpinen
  * @since 28.7.2020, v1.2
  */
object DatabaseVersionFactory extends FromValidatedRowModelFactory[DatabaseVersion]
	with FromRowFactoryWithTimestamps[DatabaseVersion]
{
	override def creationTimePropertyName = "created"
	
	override protected def fromValidatedModel(model: Model[Constant]) = DatabaseVersion(model("id"),
		DatabaseVersionData(VersionNumber.parse(model("version")), model(creationTimePropertyName)))
	
	override def table = Tables.databaseVersion
}
