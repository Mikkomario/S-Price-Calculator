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
	  * @param shopProductId Id of a shop's product description
	  * @return An access point to that description's base price
	  */
	def forShopProductWithId(shopProductId: Int) = DbSingleShopProductBasePrice(shopProductId)
	
	
	// NESTED	----------------------------
	
	case class DbSingleShopProductBasePrice(shopProductId: Int) extends SingleRowModelAccess[BasePrice]
		with UniqueAccess[BasePrice]
	{
		// IMPLEMENTED	----------------
		
		override val condition = DbProductBasePrice.mergeCondition(model.withShopProductId(shopProductId))
		
		override def factory = DbProductBasePrice.factory
		
		
		// OTHER	-------------------
		
		/**
		  * Updates the base price of this product in this shop
		  * @param newPriceData Data for the new base price
		  * @param connection DB Connection (implicit)
		  * @return Newly updated price data
		  */
		// TODO: Optimize this method (heavily) - possibly handling multiple prices at a time
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
						newPriceData.saleGroupIdentifier.flatMap { saleIdentifier =>
							DbShopProduct(shopProductId).shopId.map { shopId =>
								DbSaleGroup.inShopWithId(shopId).withIdentifier(saleIdentifier).getOrInsert
							}
						}
				}
				// If previous data was identical, won't change it
				if (newPriceData.price == existingVersion.price && sale == existingVersion.sale)
					existingVersion
				else
				{
					// Otherwise deprecates old data and inserts a new version
					model.nowDeprecated.updateWhere(model.withId(existingVersion.id).toCondition)
					model.insert(shopProductId, newPriceData.price, sale)
				}
			// Id no data existed previously, simply inserts a new version
			case None =>
				DbShopProduct(shopProductId).shopId match
				{
					case Some(shopId) => model.insert(shopId, shopProductId, newPriceData)
					case None => throw new NoSuchElementException(
						s"There doesn't exist a shop product with id $shopProductId, therefore can't set it's price either")
				}
		}
	}
}
