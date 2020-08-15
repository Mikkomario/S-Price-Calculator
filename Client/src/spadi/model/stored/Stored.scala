package spadi.model.stored

import scala.language.implicitConversions

object Stored
{
	implicit def autoUnwrap[D](s: Stored[D]): D = s.data
}

/**
  * Common trait for database-originated items
  * @author Mikko Hilpinen
  * @since 28.7.2020, v1.2
  */
trait Stored[+Data]
{
	/**
	  * @return This item's primary index in the database
	  */
	def id: Int
	
	/**
	  * @return Wrapped data
	  */
	def data: Data
}
