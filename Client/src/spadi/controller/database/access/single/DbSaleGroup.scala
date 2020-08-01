package spadi.controller.database.access.single

import spadi.controller.database.factory.pricing.SaleGroupFactory
import spadi.controller.database.model.pricing.{SaleAmountModel, SaleGroupModel}
import spadi.model.partial.pricing.SaleGroupData
import spadi.model.stored.pricing.SaleGroup
import utopia.vault.database.Connection
import utopia.vault.nosql.access.{SingleRowModelAccess, UniqueAccess}

/**
  * Used for accessing individual sale groups
  * @author Mikko Hilpinen
  * @since 1.8.2020, v1.2
  */
object DbSaleGroup extends SingleRowModelAccess[SaleGroup]
{
	// IMPLEMENTED	--------------------------
	
	override def factory = SaleGroupFactory
	
	override def globalCondition = Some(SaleGroupFactory.nonDeprecatedCondition)
	
	
	// COMPUTED	------------------------------
	
	private def model = SaleGroupModel
	
	private def amountModel = SaleAmountModel
	
	
	// OTHER	------------------------------
	
	/**
	  * @param shopId Id of the targeted shop
	  * @return An access point to individual sale groups within that shop
	  */
	def inShopWithId(shopId: Int) = DbShopSaleGroup(shopId)
	
	
	// NESTED	------------------------------
	
	case class DbShopSaleGroup(shopId: Int) extends SingleRowModelAccess[SaleGroup]
	{
		// IMPLEMENTED	----------------------
		
		override def factory = DbSaleGroup.factory
		
		override val globalCondition = Some(DbSaleGroup.mergeCondition(model.withShopId(shopId).toCondition))
		
		
		// OTHER	---------------------------
		
		/**
		  * @param identifier A sale group identifier
		  * @return An access point to unique sale group within this shop with that identifier
		  */
		def withIdentifier(identifier: String) = DbSpecificSaleGroup(identifier)
		
		
		// NESTED	---------------------------
		
		case class DbSpecificSaleGroup(identifier: String) extends SingleRowModelAccess[SaleGroup]
			with UniqueAccess[SaleGroup]
		{
			// IMPLEMENTED	-------------------
			
			override val condition = DbShopSaleGroup.this.mergeCondition(model.withIdentifier(identifier).toCondition)
			
			override def factory = DbShopSaleGroup.this.factory
			
			
			// COMPUTED	-----------------------
			
			/**
			  * @param connection Database connection (implicit)
			  * @return This sale group from the DB or one just inserted
			  */
			def getOrInsert(implicit connection: Connection) = pull.getOrElse(
				model.insert(SaleGroupData.unknownAmount(shopId, identifier)))
			
			
			// OTHER	-----------------------
			
			/**
			  * Updates the price modifier of this sale
			  * @param priceModifier New price modifier [0, 1] where 1 keeps the original price and 0 is free
			  * @param connection DB Connection (implicit)
			  * @return Newly updated sale group
			  */
			def setAmount(priceModifier: Double)(implicit connection: Connection) =
			{
				// Checks whether there exists a version to use or overwrite
				pull match
				{
					case Some(existingVersion) =>
						val groupId = existingVersion.id
						def insertAmount() =
						{
							val newAmount = amountModel.insert(groupId, priceModifier)
							existingVersion.copy(amount = Some(newAmount))
						}
						existingVersion.amount match
						{
							// Case: There already exists a specified price modifier. If not equal, overwrites it
							case Some(previousSaleAmount) =>
								if (previousSaleAmount.priceModifier == priceModifier)
									existingVersion
								else
								{
									amountModel.nowDeprecated.updateWhere(
										amountModel.withSaleGroupId(groupId).toCondition)
									insertAmount()
								}
							// If there exists a sale group without specified amount, simply specifies the amount
							case None => insertAmount()
						}
					// May need to insert the whole sale group
					case None => model.insert(SaleGroupData(shopId, identifier, priceModifier))
				}
			}
		}
	}
}
