package spadi.model.partial.pricing

/**
  * Contains basic information about a product, possibly from multiple shops
  * @author Mikko Hilpinen
  * @since 31.7.2020, v1.2
  * @param electricId Identifier unique to this product
  * @param shopData Shop specific data, each mapped to their shop's id
  */
case class ProductData(electricId: String, shopData: Map[Int, ShopProductData] = Map())
