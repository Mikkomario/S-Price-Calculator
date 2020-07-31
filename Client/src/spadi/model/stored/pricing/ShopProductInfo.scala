package spadi.model.stored.pricing

/**
  * Contains recorded information about a product specific to a single shop
  * @author Mikko Hilpinen
  * @since 31.7.2020, v1.2
  */
case class ShopProductInfo(name: ProductName, basePrice: Option[BasePrice] = None, netPrice: Option[NetPrice] = None)
