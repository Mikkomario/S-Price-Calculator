package spadi.controller.database.access.multi

import spadi.controller.database.factory.pricing.SaleGroupFactory
import spadi.model.stored.pricing.SaleGroup
import utopia.vault.nosql.access.ManyRowModelAccess

/**
  * Used for accessing multiple sale groups at once
  * @author Mikko Hilpinen
  * @since 22.8.2020, v1.2.1
  */
object DbSaleGroups extends ManyRowModelAccess[SaleGroup]
{
	// IMPLEMENTED	--------------------------
	
	override def factory = SaleGroupFactory
	
	override def globalCondition = Some(factory.nonDeprecatedCondition)
}
