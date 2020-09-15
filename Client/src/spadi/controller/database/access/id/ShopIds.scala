package spadi.controller.database.access.id

import spadi.controller.database.factory.pricing.ShopFactory
import utopia.vault.nosql.access.ManyIntIdAccess

/**
  * Used for accessing multiple shop ids at a time
  * @author Mikko Hilpinen
  * @since 13.9.2020, v1.2.3
  */
object ShopIds extends ManyIntIdAccess
{
	override def target = table
	
	override def table = ShopFactory.table
	
	override def globalCondition = None
}
