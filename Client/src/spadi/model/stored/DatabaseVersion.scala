package spadi.model.stored

import spadi.model.partial.DatabaseVersionData

/**
  * Represents a recorded database version
  * @author Mikko Hilpinen
  * @since 28.7.2020, v1.2
  */
case class DatabaseVersion(id: Int, data: DatabaseVersionData) extends Stored[DatabaseVersionData]
