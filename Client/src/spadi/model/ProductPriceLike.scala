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
}
