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
	 * @return Selling price for all units sold at once
	 */
	def totalPrice: Double
	
	/**
	 * @return Number of units sold at once (in specified unit)
	 */
	def unitsSold: Int
	
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
	 * @return Price of a single unit of this item
	 */
	def pricePerUnit = totalPrice / unitsSold
	
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
		val roundedPrice = math.round(totalPrice * priceModifier * 10) / 10.0
		val displayPrice = if (roundedPrice > 10) roundedPrice.toInt.toString else roundedPrice.toString
		val unit = if (unitsSold == 1) priceUnit else s"$unitsSold$priceUnit"
		s"$displayPrice €/$unit"
	}
}
