package spadi.controller.database.access.multi

import spadi.controller.database.factory.pricing.ShopFactory
import spadi.model.stored.pricing.Shop
import utopia.vault.nosql.access.ManyRowModelAccess

/**
  * Used for accessing multiple shops at once
  * @author Mikko Hilpinen
  * @since 2.8.2020, v1.2
  */
object DbShops extends ManyRowModelAccess[Shop]
{
	override def factory = ShopFactory
	
	override def globalCondition = None
}
