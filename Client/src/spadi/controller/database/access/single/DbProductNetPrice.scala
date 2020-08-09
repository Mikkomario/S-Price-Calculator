package spadi.controller.database.access.single

import spadi.controller.database.factory.pricing.NetPriceFactory
import spadi.controller.database.model.pricing.NetPriceModel
import spadi.model.cached.pricing.Price
import spadi.model.stored.pricing.NetPrice
import utopia.vault.database.Connection
import utopia.vault.nosql.access.{SingleRowModelAccess, UniqueAccess}

/**
  * Used for accessing net price data for individual products
  * @author Mikko Hilpinen
  * @since 2.8.2020, v1.2
  */
object DbProductNetPrice extends SingleRowModelAccess[NetPrice]
{
	// IMPLEMENTED	----------------------------
	
	override def factory = NetPriceFactory
	
	override def globalCondition = Some(factory.nonDeprecatedCondition)
	
	
	// COMPUTED	-------------------------------
	
	private def model = NetPriceModel
	
	
	// OTHER	-------------------------------
	
	/**
	  * @param shopProductId A shop product description's id
	  * @return An access point to that product's net price
	  */
	def forShopProductWithId(shopProductId: Int) = DbSingleShopProductNetPrice(shopProductId)
	
	
	// NESTED	-------------------------------
	
	case class DbSingleShopProductNetPrice(shopProductId: Int)
		extends SingleRowModelAccess[NetPrice] with UniqueAccess[NetPrice]
	{
		// IMPLEMENTED	-------------------
		
		override lazy val condition = DbProductNetPrice.mergeCondition(model.withShopProductId(shopProductId))
		
		override def factory = DbProductNetPrice.factory
		
		
		// OTHER	-----------------------
		
		/**
		  * Updates the net price of this product
		  * @param newPrice New net price for this product
		  * @param connection DB Connection (implicit)
		  * @return Newly inserted or existing identical price data
		  */
		// Checks for existing data first, deprecates if present and different. Just inserts otherwise.
		def set(newPrice: Price)(implicit connection: Connection) = pull match
		{
			case Some(existingVersion) =>
				if (existingVersion.price == newPrice)
					existingVersion
				else
				{
					model.nowDeprecated.updateWhere(model.withId(existingVersion.id).toCondition)
					model.insert(shopProductId, newPrice)
				}
			case None => model.insert(shopProductId, newPrice)
		}
	}
}
