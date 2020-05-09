package spadi.model

import scala.math.Ordering.Double.TotalOrdering

/**
 * Contains all price information about a single product
 * @author Mikko Hilpinen
 * @since 9.5.2020, v1
 * @param id Product id
 * @param prices Available prices for this product (Must not be empty)
 */
case class Product(id: String, prices: Set[ProductSalePrice])
{
	// ATTRIBUTES   --------------------------
	
	private val cheapest = prices.minBy { _.price }
	
	
	// COMPUTED ------------------------------
	
	/**
	 * @return Name that should be displayed for this product
	 */
	def displayName = cheapest.displayName
	
	/**
	 * @return Name of the producer of this product
	 */
	def producer = cheapest.sale.flatMap { _.producerName }
	
	/**
	 * @return Price of this product
	 */
	def price = cheapest.price
	
	/**
	 * @return A string representation of this product's price (unit included)
	 */
	def standardPriceString = priceString(1.0)
	
	
	// OTHER    ------------------------------
	
	/**
	 * @param search Search words
	 * @return How well this product matches specified search words
	 */
	def matches(search: Set[String]) = (prices.foldLeft(0) { _ + _.matches(search) } / prices.size) +
		cheapest.matches(search)
	
	/**
	 * Forms a string representation of this product's price
	 * @param priceModifier A modifier applied to the price
	 * @return String representation of product price
	 */
	def priceString(priceModifier: Double) =
	{
		val roundedPrice = math.round(cheapest.price * priceModifier * 10) / 10.0
		val displayPrice = if (roundedPrice > 10) roundedPrice.toInt.toString else roundedPrice.toString
		s"$displayPrice ${cheapest.basePrice.priceUnit}"
	}
}
