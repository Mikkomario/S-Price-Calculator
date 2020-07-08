package spadi.model

import utopia.flow.datastructure.immutable.{Model, ModelDeclaration}
import utopia.flow.datastructure.template
import utopia.flow.datastructure.template.Property
import utopia.flow.generic.{DoubleType, FromModelFactory, ModelConvertible, ModelType, StringType}
import utopia.flow.generic.ValueConversions._
import utopia.flow.generic.ValueUnwraps._

object ProductBasePrice extends FromModelFactory[ProductBasePrice]
{
	private val priceSchema = ModelDeclaration("amount" -> DoubleType, "unit" -> StringType)
	private val schema = ModelDeclaration("id" -> StringType, "price" -> ModelType)
	
	override def apply(model: template.Model[Property]) = schema.validate(model).toTry.flatMap { valid =>
		priceSchema.validate(valid("price").getModel).toTry.map { priceModel =>
			ProductBasePrice(valid("id"), valid("group_id"), valid("names").getVector.flatMap { _.string },
				priceModel("amount"), valid("item_count").intOr(1), priceModel("unit"))
		}
	}
}

/**
 * Represents an electrical product price combination (before applying any sales or other bonuses)
 * @author Mikko Hilpinen
 * @since 8.5.2020, v1
 * @param productId Product identifier id
 * @param salesGroupId Id of the sales group this product belongs to. None if this product doesn't have a sales group.
 * @param names Names for this product
 * @param totalPrice Price of this product package
 * @param unitsSold Number of items sold as a package
 * @param priceUnit Unit describing the price of this product
 */
case class ProductBasePrice(productId: String, salesGroupId: Option[String], names: Vector[String], totalPrice: Double,
                            unitsSold: Int, priceUnit: String)
	extends ModelConvertible with KeywordSearchable with ProductPriceLike
{
	// IMPLEMENTED  ----------------------------------
	
	override val keywords = (productId +: names).map { _.toLowerCase }
	
	def displayName = names.headOption.getOrElse("")
	
	override def toModel =
	{
		val priceModel = Model(Vector("amount" -> totalPrice, "unit" -> priceUnit))
		Model(Vector("id" -> productId, "group_id" -> salesGroupId, "names" -> names, "price" -> priceModel,
			"item_count" -> unitsSold))
	}
}
