package spadi.controller.database.access.multi

import spadi.controller.database.factory.reading.PriceKeyMappingFactory
import spadi.controller.database.model.reading.PriceKeyMappingModel
import spadi.model.enumeration.PriceType
import spadi.model.enumeration.PriceType.{Base, Net}
import spadi.model.partial.reading.PriceKeyMappingData
import spadi.model.stored.reading.PriceKeyMapping
import utopia.vault.database.Connection
import utopia.vault.nosql.access.ManyRowModelAccess

/**
  * Used for accessing multiple price key mappings at a time
  * @author Mikko Hilpinen
  * @since 3.8.2020, v1.2
  */
object DbPriceKeyMappings extends ManyRowModelAccess[PriceKeyMapping]
{
	// IMPLEMENTED	---------------------------
	
	override def factory = PriceKeyMappingFactory
	
	override def globalCondition = None
	
	
	// COMPUTED	-------------------------------
	
	private def model = PriceKeyMappingModel
	
	
	// OTHER	------------------------------
	
	/**
	  * @param shopId Id of targeted shop
	  * @return An access point to that shop's price key mappings
	  */
	def forShopWithId(shopId: Int) = DbShopPriceKeyMappings(shopId)
	
	/**
	  * Inserts a new mapping to DB
	  * @param data Mapping to insert
	  * @param connection DB Connection (implicit)
	  * @return Newly inserted mapping
	  */
	def insert(data: PriceKeyMappingData)(implicit connection: Connection) = model.insert(data)
	
	
	// NESTED	------------------------------
	
	case class DbShopPriceKeyMappings(shopId: Int) extends ManyRowModelAccess[PriceKeyMapping]
	{
		// IMPLEMENTED	----------------------
		
		override def factory = DbPriceKeyMappings.factory
		
		override def globalCondition = Some(DbPriceKeyMappings.mergeCondition(model.withShopId(shopId)))
		
		
		// COMPUTED	--------------------------
		
		/**
		  * @return An access point to this shop's net price mappings
		  */
		def forNetPrices = forPriceType(Net)
		
		/**
		  * @return An access point to this shop's base price mappings
		  */
		def forBasePrices = forPriceType(Base)
		
		
		// OTHER	--------------------------
		
		/**
		  * @param priceType Type of price document
		  * @return An access point to mappings of that type for this shop
		  */
		def forPriceType(priceType: PriceType) = DbSpecificShopPriceKeyMappings(priceType)
		
		
		// NESTED	--------------------------
		
		case class DbSpecificShopPriceKeyMappings(mappingType: PriceType) extends ManyRowModelAccess[PriceKeyMapping]
		{
			// IMPLEMENTED	------------------
			
			override def factory = DbShopPriceKeyMappings.this.factory
			
			override def globalCondition =
				Some(DbShopPriceKeyMappings.this.mergeCondition(model.withPriceType(mappingType)))
		}
	}
}
