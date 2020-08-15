package spadi.controller.database.access.id

import spadi.controller.database.factory.pricing.SaleGroupFactory
import spadi.controller.database.model.pricing.SaleGroupModel
import utopia.vault.nosql.access.{SingleIntIdAccess, UniqueAccess}

/**
  * Used for accessing individual sale group ids within DB
  * @author Mikko Hilpinen
  * @since 1.8.2020, v1.2
  */
object SaleGroupId extends SingleIntIdAccess
{
	// IMPLEMENTED	--------------------------
	
	// Won't include sale amounts in these queries
	override def target = factory.table
	
	override def table = factory.table
	
	override def globalCondition = None
	
	
	// COMPUTED	-------------------------------
	
	private def factory = SaleGroupFactory
	
	
	// OTHER	-------------------------------
	
	/**
	  * @param shopId Id of targeted shop
	  * @return An access point to that shop's individual sale group ids
	  */
	def forShopWithId(shopId: Int) = ShopSaleGroupId(shopId)
	
	
	// NESTED	-------------------------------
	
	case class ShopSaleGroupId(shopId: Int) extends SingleIntIdAccess
	{
		// IMPLEMENTED	-----------------------
		
		override def target = SaleGroupId.target
		
		override def table = SaleGroupId.table
		
		override val globalCondition = Some(SaleGroupId.mergeCondition(model.withShopId(shopId).toCondition))
		
		
		// COMPUTED	---------------------------
		
		private def model = SaleGroupModel
		
		
		// OTHER	---------------------------
		
		/**
		  * @param groupIdentifier A sale group identifier
		  * @return An access point to the sale group in this shop that has the specified identifier
		  */
		def withIdentifier(groupIdentifier: String) = UniqueSaleGroupId(groupIdentifier)
		
		
		// NESTED	---------------------------
		
		case class UniqueSaleGroupId(identifier: String) extends SingleIntIdAccess with UniqueAccess[Int]
		{
			override val condition = ShopSaleGroupId.this.mergeCondition(model.withIdentifier(identifier).toCondition)
			
			override def target = ShopSaleGroupId.this.target
			
			override def table = ShopSaleGroupId.this.table
		}
	}
}
