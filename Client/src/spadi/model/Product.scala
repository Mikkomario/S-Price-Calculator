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
	 * @return A string representation of this product's price (unit included)
	 */
	def priceString =
	{
		val roundedPrice = (cheapest.price * 100).toInt / 100
		s"$roundedPrice ${cheapest.basePrice.priceUnit}"
	}
}
