package spadi.model

import utopia.flow.datastructure.immutable.{Model, ModelDeclaration}
import utopia.flow.datastructure.template
import utopia.flow.datastructure.template.Property
import utopia.flow.generic.{DoubleType, FromModelFactory, ModelConvertible, ModelType, StringType}
import utopia.flow.generic.ValueConversions._

object ProductBasePrice extends FromModelFactory[ProductBasePrice]
{
	private val priceSchema = ModelDeclaration("amount" -> DoubleType, "unit" -> StringType)
	private val schema = ModelDeclaration("id" -> StringType, "group_id" -> StringType, "price" -> ModelType)
	
	override def apply(model: template.Model[Property]) = schema.validate(model).toTry.flatMap { valid =>
		priceSchema.validate(valid("price").getModel).toTry.map { priceModel =>
			ProductBasePrice(valid("id").getString, valid("group_id").getString,
				valid("names").getVector.flatMap { _.string }, priceModel("amount").getDouble, priceModel("unit").getString)
		}
	}
}

/**
 * Represents an electrical product price combination (before applying any sales or other bonuses)
 * @author Mikko Hilpinen
 * @since 8.5.2020, v1
 * @param productId Product identifier id
 * @param salesGroupId Id of the sales group this product belongs to
 * @param names Names for this product
 * @param price Price of this product
 * @param priceUnit Unit describing the price of this product
 */
case class ProductBasePrice(productId: String, salesGroupId: String, names: Vector[String], price: Double, priceUnit: String)
	extends ModelConvertible with KeywordSearchable with ProductPriceLike
{
	// IMPLEMENTED  ----------------------------------
	
	override val keywords = (productId +: names).map { _.toLowerCase }
	
	def displayName = names.headOption.getOrElse("")
	
	override def toModel =
	{
		val priceModel = Model(Vector("amount" -> price, "unit" -> priceUnit))
		Model(Vector("id" -> productId, "group_id" -> salesGroupId, "names" -> names, "price" -> priceModel))
	}
}
