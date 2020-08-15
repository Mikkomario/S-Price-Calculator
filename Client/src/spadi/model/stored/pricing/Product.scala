package spadi.model.stored.pricing

import spadi.model.cached.pricing.Price

/**
  * Represents a stored product's information, including shop-specific data
  * @author Mikko Hilpinen
  * @since 31.7.2020, v1.2
  */
case class Product(id: Int, electricId: String, shopData: Set[ShopProduct] = Set())
{
	// ATTRIBUTES	------------------------
	
	private val cheapestData = shopData.minByOption { _.price.getOrElse(Price.max) }
	
	
	// COMPUTED	----------------------------
	
	/**
	  * @return Cheapest available price for this product. None if no price is listed.
	  */
	def cheapestPrice = cheapestData.flatMap { _.price }
	
	/**
	  * @return Prices available for this product besides the cheapest price. May be empty.
	  */
	def alternativePrices =
	{
		val prices = shopData.flatMap { _.price }.toVector.sorted
		if (prices.size > 1)
			prices.tail
		else
			Vector()
	}
	
	/**
	  * @return Id of the shop that offers the cheapest price for this product. None if no shop data is known.
	  */
	def cheapestShopId = cheapestData.map { _.shopId }
	
	/**
	  * @return This product's name
	  */
	def name = cheapestData.map { _.name }.getOrElse("Nimeämätön tuote")
}
