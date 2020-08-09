package spadi.model.partial.pricing

@deprecated("Replaced with shopProductData", "v1.2")
object ProductData
{
	/**
	  * Creates a new product data that only contains a single shop's data
	  * @param electricId Electric identifier of this product
	  * @param shopId Id of the shop describing this product
	  * @param data Shop's description for this product
	  * @return A new product data instance
	  */
	def apply(electricId: String, shopId: Int, data: ShopProductData): ProductData =
		apply(electricId, Map(shopId -> data))
}

/**
  * Contains basic information about a product, possibly from multiple shops
  * @author Mikko Hilpinen
  * @since 31.7.2020, v1.2
  * @param electricId Identifier unique to this product
  * @param shopData Shop specific data, each mapped to their shop's id
  */
@deprecated("Replaced with shopProductData", "v1.2")
case class ProductData(electricId: String, shopData: Map[Int, ShopProductData] = Map())
