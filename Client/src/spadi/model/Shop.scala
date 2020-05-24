package spadi.model

import utopia.flow.datastructure.immutable.{Constant, Model, ModelDeclaration}
import utopia.flow.generic.{FromModelFactoryWithSchema, ModelConvertible, StringType}
import utopia.flow.generic.ValueConversions._
import utopia.flow.generic.ValueUnwraps._

object Shop extends FromModelFactoryWithSchema[Shop]
{
	override val schema = ModelDeclaration("id" -> StringType, "name" -> StringType)
	
	override protected def fromValidatedModel(model: Model[Constant]) = Shop(model("id"), model("name"))
}

/**
 * Represents a shop you can buy products from
 * @author Mikko Hilpinen
 * @since 21.5.2020, v1.1
 * @param id Unique shop id
 * @param name Current name of this shop
 */
case class Shop(id: String, name: String) extends ModelConvertible
{
	override def toModel = Model(Vector("id" -> id, "name" -> name))
}
