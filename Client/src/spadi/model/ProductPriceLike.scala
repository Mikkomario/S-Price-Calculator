package spadi.model

/**
 * Common trait for products with prices (may not be final prices)
 * @author Mikko Hilpinen
 * @since 21.5.2020, v2
 */
trait ProductPriceLike
{
	// ABSTRACT ------------------------
	
	/**
	 * @return Product's id
	 */
	def productId: String
	
	/**
	 * @return Product's price
	 */
	def price: Double
	
	/**
	 * @return Unit describing the product's price
	 */
	def priceUnit: String
	
	/**
	 * @return Name displayed for this product
	 */
	def displayName: String
	
	
	// COMPUTED --------------------------
	
	/**
	 * @return A string representation of this product's price (unit included)
	 */
	def standardPriceString = priceString(1.0)
	
	
	// OTHER    --------------------------
	
	/**
	 * Forms a string representation of this product's price
	 * @param priceModifier A modifier applied to the price
	 * @return String representation of product price
	 */
	def priceString(priceModifier: Double) =
	{
		val roundedPrice = math.round(price * priceModifier * 10) / 10.0
		val displayPrice = if (roundedPrice > 10) roundedPrice.toInt.toString else roundedPrice.toString
		s"$displayPrice $priceUnit"
	}
}
