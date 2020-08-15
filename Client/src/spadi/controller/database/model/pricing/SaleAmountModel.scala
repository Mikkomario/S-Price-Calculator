package spadi.controller.database.model.pricing

import java.time.Instant

import spadi.controller.database.factory.pricing.SaleAmountFactory
import spadi.model.stored.pricing.SaleAmount
import utopia.flow.generic.ValueConversions._
import utopia.vault.database.Connection
import utopia.vault.model.immutable.StorableWithFactory

object SaleAmountModel
{
	// COMPUTED	----------------------------
	
	/**
	  * @return A model that has just been marked as deprecated
	  */
	def nowDeprecated = apply(deprecatedAfter = Some(Instant.now()))
	
	
	// OTHER	----------------------------
	
	/**
	  * @param groupId Id of the described sale group
	  * @return A model with only sale group id set
	  */
	def withSaleGroupId(groupId: Int) = apply(groupId = Some(groupId))
	
	/**
	  * Inserts a new sale amount to the database
	  * @param groupId Id of affected sale group
	  * @param priceModifier Modifier applied to product price [0, 1]
	  * @param connection DB Connection (implicit)
	  * @return Newly inserted sale amount
	  */
	def insert(groupId: Int, priceModifier: Double)(implicit connection: Connection) =
	{
		val id = apply(None, Some(groupId), Some(priceModifier)).insert().getInt
		SaleAmount(id, groupId, priceModifier)
	}
}

/**
  * Used for interacting with sale amount data in DB
  * @author Mikko Hilpinen
  * @since 1.8.2020, v1.2
  */
case class SaleAmountModel(id: Option[Int] = None, groupId: Option[Int] = None, priceModifier: Option[Double] = None,
						   deprecatedAfter: Option[Instant] = None) extends StorableWithFactory[SaleAmount]
{
	override def factory = SaleAmountFactory
	
	override def valueProperties = Vector("id" -> id, "groupId" -> groupId, "priceModifier" -> priceModifier,
		"deprecatedAfter" -> deprecatedAfter)
}
