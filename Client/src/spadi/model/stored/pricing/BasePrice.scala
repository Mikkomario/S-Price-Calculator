package spadi.model.stored.pricing

import spadi.model.partial.pricing.BasePriceData
import spadi.model.stored.Stored

/**
  * Represents a recorded product's standard price in a shop
  * @author Mikko Hilpinen
  * @since 31.7.2020, v1.2
  */
case class BasePrice(id: Int, productId: Int, shopId: Int, data: BasePriceData, sale: Option[SaleGroup] = None)
	extends Stored[BasePriceData]
