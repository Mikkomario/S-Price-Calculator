package spadi.controller.database.model.pricing

import spadi.controller.database.factory.pricing.SaleGroupFactory
import spadi.model.partial.pricing.SaleGroupData
import spadi.model.stored.pricing.SaleGroup
import utopia.flow.generic.ValueConversions._
import utopia.vault.database.Connection
import utopia.vault.model.immutable.StorableWithFactory

object SaleGroupModel
{
	// OTHER	----------------------------
	
	/**
	  * @param shopId Id of shop where the sale is held
	  * @return A model with only shop id set
	  */
	def withShopId(shopId: Int) = apply(shopId = Some(shopId))
	
	/**
	  * @param groupIdentifier Sale group identifier. Unique within shop.
	  * @return A model with only identifier set
	  */
	def withIdentifier(groupIdentifier: String) = apply(groupIdentifier = Some(groupIdentifier))
	
	/**
	  * Inserts a new sale group to the DB
	  * @param data Data to insert
	  * @param connection DB Connection (implicit)
	  * @return newly inserted group
	  */
	def insert(data: SaleGroupData)(implicit connection: Connection) =
	{
		// Inserts group first, then sale amount
		val id = apply(None, Some(data.shopId), Some(data.groupIdentifier)).insert().getInt
		val amount = SaleAmountModel.insert(id, data.priceModifier)
		SaleGroup(id, data.shopId, data.groupIdentifier, amount)
	}
}

/**
  * Used for interacting with sale groups in DB
  * @author Mikko Hilpinen
  * @since 1.8.2020, v1.2
  */
case class SaleGroupModel(id: Option[Int] = None, shopId: Option[Int] = None, groupIdentifier: Option[String] = None)
	extends StorableWithFactory[SaleGroup]
{
	// IMPLEMENTED	---------------------------
	
	override def factory = SaleGroupFactory
	
	override def valueProperties = Vector("id" -> id, "shopId" -> shopId, "groupIdentifier" -> groupIdentifier)
	
	
	// OTHER	-------------------------------
	
	/**
	  * @param identifier A sale group identifier
	  * @return A copy of this model with specified identifier
	  */
	def withGroupIdentifier(identifier: String) = copy(groupIdentifier = Some(identifier))
}
