package spadi.model.stored

import spadi.model.Extender

import scala.language.implicitConversions

/**
  * Common trait for database-originated items
  * @author Mikko Hilpinen
  * @since 28.7.2020, v1.2
  */
trait Stored[+Data] extends Extender[Data]
{
	// ABSTRACT	--------------------------------
	
	/**
	  * @return This item's primary index in the database
	  */
	def id: Int
	
	/**
	  * @return Wrapped data
	  */
	def data: Data
	
	
	// IMPLEMENTED	---------------------------
	
	override def wrapped = data
}
