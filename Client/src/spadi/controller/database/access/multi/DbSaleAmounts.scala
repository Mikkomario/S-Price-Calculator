package spadi.controller.database.access.multi

import spadi.controller.database.factory.pricing.SaleAmountFactory
import spadi.model.stored.pricing.SaleAmount
import utopia.vault.database.Connection
import utopia.vault.nosql.access.ManyRowModelAccess
import utopia.vault.sql.{Delete, Where}

/**
  * Used for accessing multiple sale amounts at once
  * @author Mikko Hilpinen
  * @since 22.8.2020, v1.2.1
  */
object DbSaleAmounts extends ManyRowModelAccess[SaleAmount]
{
	// IMPLEMENTED	-------------------------
	
	override def factory = SaleAmountFactory
	
	override def globalCondition = Some(factory.nonDeprecatedCondition)
	
	
	// OTHER	-----------------------------
	
	/**
	  * Deletes all deprecated sale amounts
	  * @param connection DB Connection (implicit)
	  */
	def deleteDeprecatedData()(implicit connection: Connection): Unit =
		connection(Delete(table) + Where(factory.deprecationColumn.isNotNull))
}
