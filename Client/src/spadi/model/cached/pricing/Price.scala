package spadi.model.cached.pricing

import utopia.flow.util.RichComparable
import utopia.genesis.util.Scalable

object Price
{
	/**
	  * Maximum price possible
	  */
	val max = apply(Double.MaxValue)
}

/**
  * Represents a product price. Consists of the total price, a unit and the number of items sold at once
  * @author Mikko Hilpinen
  * @since 31.7.2020, v1.2
  */
case class Price(amount: Double, unit: String = "kpl", unitsSold: Int = 1) extends RichComparable[Price]
	with Scalable[Price]
{
	// COMPUTED	-------------------------
	
	/**
	  * @return Price for each single unit
	  */
	def pricePerUnit = amount / unitsSold
	
	
	// IMPLEMENTED	---------------------
	
	override def repr = this
	
	override def *(mod: Double) = copy(amount = amount * mod)
	
	override def compareTo(o: Price) = pricePerUnit.compareTo(o.pricePerUnit)
	
	override def toString = s"${if (amount > 10) amount.toInt else
		(amount * 10).toInt / 10} €/${if (unitsSold == 1) "" else unitsSold}$unit"
}
