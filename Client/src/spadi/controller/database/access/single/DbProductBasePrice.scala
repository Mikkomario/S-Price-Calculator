package spadi.controller.database.access.single

import spadi.controller.database.factory.pricing.BasePriceFactory
import spadi.controller.database.model.pricing.BasePriceModel
import spadi.model.partial.pricing.BasePriceData
import spadi.model.stored.pricing.BasePrice
import utopia.vault.database.Connection
import utopia.vault.nosql.access.{SingleRowModelAccess, UniqueAccess}

/**
  * Used for accessing individual product base prices
  * @author Mikko Hilpinen
  * @since 2.8.2020, v1.2
  */
object DbProductBasePrice extends SingleRowModelAccess[BasePrice]
{
	// IMPLEMENTED	------------------------
	
	override def factory = BasePriceFactory
	
	override def globalCondition = Some(factory.nonDeprecatedCondition)
	
	
	// COMPUTED	----------------------------
	
	private def model = BasePriceModel
	
	
	// OTHER	----------------------------
	
	/**
	  * @param productId Id of the described product
	  * @return An access point to that product's base prices
	  */
	def forProductWithId(productId: Int) = DbSingleProductBasePrice(productId)
	
	
	// NESTED	----------------------------
	
	case class DbSingleProductBasePrice(productId: Int) extends SingleRowModelAccess[BasePrice]
	{
		// IMPLEMENTED	--------------------
		
		override def factory = DbProductBasePrice.factory
		
		override lazy val globalCondition = Some(DbProductBasePrice.mergeCondition(
			model.withProductId(productId)))
		
		
		// OTHER	------------------------
		
		/**
		  * @param shopId Id of the targeted shop
		  * @return An access point to this product's base price in that shop
		  */
		def inShopWithId(shopId: Int) = DbSingleShopProductBasePrice(shopId)
		
		
		// NESTED	------------------------
		
		case class DbSingleShopProductBasePrice(shopId: Int) extends SingleRowModelAccess[BasePrice]
			with UniqueAccess[BasePrice]
		{
			// IMPLEMENTED	----------------
			
			override val condition = DbSingleProductBasePrice.this.mergeCondition(model.withShopId(shopId))
			
			override def factory = DbSingleProductBasePrice.this.factory
			
			
			// OTHER	-------------------
			
			/**
			  * Updates the base price of this product in this shop
			  * @param newPriceData Data for the new base price
			  * @param connection DB Connection (implicit)
			  * @return Newly updated price data
			  */
			// Checks for existing price data
			def set(newPriceData: BasePriceData)(implicit connection: Connection) = pull match
			{
				case Some(existingVersion) =>
					// Uses existing sale, if possible
					val sale =
					{
						if (existingVersion.sale.map { _.groupIdentifier } == newPriceData.saleGroupIdentifier)
							existingVersion.sale
						else
							newPriceData.saleGroupIdentifier.map { saleIdentifier =>
								DbSaleGroup.inShopWithId(shopId).withIdentifier(saleIdentifier).getOrInsert
							}
					}
					// If previous data was identical, won't change it
					if (newPriceData.price == existingVersion.price && sale == existingVersion.sale)
						existingVersion
					else
					{
						// Otherwise deprecates old data and inserts a new version
						model.nowDeprecated.updateWhere(model.withId(existingVersion.id).toCondition)
						model.insert(productId, shopId, newPriceData.price, sale)
					}
				// Id no data existed previously, simply inserts a new version
				case None => model.insert(productId, shopId, newPriceData)
			}
		}
	}
}
