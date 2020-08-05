package spadi.model.stored.pricing

import spadi.model.cached.pricing.Price

/**
  * Represents a stored product's information, including shop-specific data
  * @author Mikko Hilpinen
  * @since 31.7.2020, v1.2
  */
case class Product(id: Int, electricId: String, shopData: Map[Int, ShopProductInfo] = Map())
{
	// ATTRIBUTES	------------------------
	
	private val cheapestInfo = shopData.minByOption { case (_, info) => info.price.getOrElse(Price.max) }
	
	
	// COMPUTED	----------------------------
	
	/**
	  * @return Cheapest available price for this product. None if no price is listed.
	  */
	def cheapestPrice = cheapestInfo.flatMap { _._2.price }
	
	/**
	  * @return Id of the shop that offers the cheapest price for this product. None if no shop data is known.
	  */
	def cheapestShopId = cheapestInfo.map { _._1 }
	
	/**
	  * @return This product's name
	  */
	def name = cheapestInfo.map { _._2.name.name }.getOrElse("Nimeämätön tuote")
}
