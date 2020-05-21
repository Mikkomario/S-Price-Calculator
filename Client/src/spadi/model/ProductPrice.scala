package spadi.model

import utopia.flow.datastructure.immutable.{Model, ModelDeclaration}
import utopia.flow.datastructure.template
import utopia.flow.datastructure.template.Property
import utopia.flow.generic.{DoubleType, FromModelFactory, ModelConvertible, ModelType, StringType}
import utopia.flow.generic.ValueConversions._
import utopia.flow.generic.ValueUnwraps._

object ProductPrice extends FromModelFactory[ProductPrice]
{
	// ATTRIBUTES   -------------------------------
	
	private val priceSchema = ModelDeclaration("amount" -> DoubleType, "unit" -> StringType)
	private val schema = ModelDeclaration("id" -> StringType, "price" -> ModelType)
	
	
	// IMPLEMENTED  -------------------------------
	
	override def apply(model: template.Model[Property]) = schema.validate(model).toTry.flatMap { valid =>
		priceSchema.validate(valid("price").getModel).toTry.map { priceModel =>
			ProductPrice(valid("id"), valid("names").getVector.flatMap { _.string },
				priceModel("amount"), priceModel("unit"))
		}
	}
}

/**
 * Represents a product's price that includes sale %
 * @author Mikko Hilpinen
 * @since 21.5.2020, v1.1
 * @param productId Id of this product
 * @param names Names describing this product
 * @param price Product price
 * @param priceUnit Unit describing the product's price
 */
case class ProductPrice(productId: String, names: Vector[String], price: Double, priceUnit: String)
	extends ModelConvertible with KeywordSearchable with ProductPriceLike
{
	override val keywords = (productId +: names).map { _.toLowerCase }
	
	override def toModel =
	{
		val priceModel = Model(Vector("amount" -> price, "unit" -> priceUnit))
		Model(Vector("id" -> productId, "names" -> names, "price" -> priceModel))
	}
}
