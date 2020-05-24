package spadi.model

import java.nio.file.Path

import utopia.flow.datastructure.immutable.{Model, ModelDeclaration}
import utopia.flow.datastructure.template
import utopia.flow.datastructure.template.Property
import utopia.flow.generic.{FromModelFactory, ModelConvertible, ModelType, StringType}
import utopia.flow.generic.ValueConversions._
import utopia.flow.util.FileExtensions._

object DataSource
{
	// ATTRIBUTES   ------------------------------
	
	private val schema = ModelDeclaration("path" -> StringType, "mapping" -> ModelType)
	
	/**
	 * A data source factory compatible with product prices
	 */
	val factoryForProductPrices = factoryWith(ProductPriceKeyMapping)
	
	/**
	 * A data source factory compatible with product base prices
	 */
	val factoryForBasePrices = factoryWith(ProductBasePriceKeyMapping)
	
	/**
	 * A data source factory compatible with sales groups
	 */
	val factoryForSalesGroups = factoryWith(SalesGroupKeyMapping)
	
	
	// OTHER    ----------------------------------
	
	/**
	 * @param mappingFactory A key mapping factory
	 * @tparam A Type of produced item
	 * @tparam M Type of mapping factory
	 * @return A new model to data source parser that uses the specified mapping factory when parsing models
	 */
	def factoryWith[A, M <: KeyMapping[A]](mappingFactory: FromModelFactory[M]): FromModelFactory[DataSource[A]] =
		Factory(mappingFactory)
	
	
	// NESTED   ----------------------------------
	
	private case class Factory[+A, +M <: KeyMapping[A]](mappingFactory: FromModelFactory[M])
		extends FromModelFactory[DataSource[A]]
	{
		override def apply(model: template.Model[Property]) = schema.validate(model).toTry.flatMap { valid =>
			mappingFactory(valid("mapping").getModel).map { DataSource(valid("path").getString, _) } }
	}
}

/**
 * Describes a file data is read from
 * @author Mikko Hilpinen
 * @since 21.5.2020, v1.1
 */
case class DataSource[+A](filePath: Path, mapping: KeyMapping[A]) extends ModelConvertible
{
	override def toModel = Model(Vector("path" -> filePath.toJson, "mapping" -> mapping.toModel))
}
