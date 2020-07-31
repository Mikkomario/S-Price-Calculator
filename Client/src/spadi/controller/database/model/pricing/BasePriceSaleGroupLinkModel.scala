package spadi.controller.database.model.pricing

import spadi.controller.database.Tables
import utopia.vault.model.immutable.Storable
import utopia.flow.generic.ValueConversions._
import utopia.vault.database.Connection

object BasePriceSaleGroupLinkModel
{
	// COMPUTED	------------------------
	
	/**
	  * @return Table used by this model type
	  */
	def table = Tables.basePriceSaleLink
	
	
	// OTHER	------------------------
	
	/**
	  * Links a base price with a sale group
	  * @param basePriceId Base price id
	  * @param saleGroupId Sale group id
	  * @param connection DB Connection (implicit)
	  * @return Id of the newly inserted link
	  */
	def insert(basePriceId: Int, saleGroupId: Int)(implicit connection: Connection) =
		apply(None, Some(basePriceId), Some(saleGroupId)).insert().getInt
}

/**
  * Used for interacting with database links between product base prices and associated sale groups
  * @author Mikko Hilpinen
  * @since 1.8.2020, v1.2
  */
case class BasePriceSaleGroupLinkModel(id: Option[Int] = None, basePriceId: Option[Int] = None,
									   saleId: Option[Int] = None) extends Storable
{
	override def table = BasePriceSaleGroupLinkModel.table
	
	override def valueProperties = Vector("id" -> id, "basePriceId" -> basePriceId, "saleId" -> saleId)
}
