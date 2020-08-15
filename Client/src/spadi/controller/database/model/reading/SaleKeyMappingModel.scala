package spadi.controller.database.model.reading

import spadi.controller.database.factory.reading.SaleKeyMappingFactory
import spadi.model.partial.reading.SaleKeyMappingData
import spadi.model.stored.reading.SaleKeyMapping
import utopia.flow.generic.ValueConversions._
import utopia.vault.database.Connection
import utopia.vault.model.immutable.StorableWithFactory

object SaleKeyMappingModel
{
	/**
	  * @param shopId Id of the shop that gives the sale
	  * @return A model with only shop id set
	  */
	def withShopId(shopId: Int) = apply(shopId = Some(shopId))
	
	/**
	  * Inserts a new sale key mapping to DB
	  * @param data Data to insert
	  * @param connection DB Connection (implicit)
	  * @return Newly inserted mapping
	  */
	def insert(data: SaleKeyMappingData)(implicit connection: Connection) =
	{
		val id = apply(None, Some(data.shopId), Some(data.groupIdKey), Some(data.salePercentKey)).insert().getInt
		SaleKeyMapping(id, data)
	}
}

/**
  * Used for interacting with sale key mappings in DB
  * @author Mikko Hilpinen
  * @since 3.8.2020, v1.2
  */
case class SaleKeyMappingModel(id: Option[Int] = None, shopId: Option[Int] = None, groupIdKey: Option[String] = None,
							   salePercentKey: Option[String] = None) extends StorableWithFactory[SaleKeyMapping]
{
	override def factory = SaleKeyMappingFactory
	
	override def valueProperties = Vector("id" -> id, "shopId" -> shopId, "groupIdKey" -> groupIdKey,
		"salePercentKey" -> salePercentKey)
}
