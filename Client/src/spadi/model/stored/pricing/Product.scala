package spadi.model.stored.pricing

/**
  * Represents a stored product's information, including shop-specific data
  * @author Mikko Hilpinen
  * @since 31.7.2020, v1.2
  */
case class Product(id: Int, electricId: String, shopData: Map[Int, ShopProductInfo] = Map())
