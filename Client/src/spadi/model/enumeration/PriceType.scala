package spadi.model.enumeration

import utopia.flow.util.CollectionExtensions._

/**
  * An enumeration for different pricing styles
  * @author Mikko Hilpinen
  * @since 3.8.2020, v1.2
  */
sealed trait PriceType
{
	/**
	  * @return A unique id for this price type
	  */
	def id: Int
}

object PriceType
{
	// ATTRIBUTES	------------------------
	
	/**
	  * All known price type values
	  */
	val values = Vector[PriceType](Net, Base)
	
	
	// OTHER	----------------------------
	
	/**
	  * @param typeId Price type id
	  * @return A price type matching that id. Failure if not found.
	  */
	def forId(typeId: Int) = values.find { _.id == typeId }.toTry {
		new NoSuchElementException(s"No price type for id $typeId") }
	
	
	// NESTED	----------------------------
	
	/**
	  * Final product price
	  */
	case object Net extends PriceType
	{
		override def id = 1
	}
	
	/**
	  * Product price before sales are applied
	  */
	case object Base extends PriceType
	{
		override def id = 2
	}
}
