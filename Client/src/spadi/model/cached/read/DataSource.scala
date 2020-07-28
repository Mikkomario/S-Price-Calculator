package spadi.model.cached.read

import java.nio.file.Path

import spadi.model._
import spadi.model.cached.pricing.product.{ProductBasePrice, ProductPrice, SalesGroup}
import utopia.flow.datastructure.immutable.{Model, ModelDeclaration}
import utopia.flow.datastructure.template
import utopia.flow.datastructure.template.Property
import utopia.flow.generic.ValueConversions._
import utopia.flow.generic.{FromModelFactory, ModelConvertible, ModelType, StringType}
import utopia.flow.util.FileExtensions._

object DataSource
{
	// ATTRIBUTES   ------------------------------
	
	private val schema = ModelDeclaration("path" -> StringType, "mapping" -> ModelType)
	
	/**
	 * A data source factory compatible with product prices
	 */
	val factoryForProductPrices = factoryWith[ProductPrice, ProductPriceKeyMapping](ProductPriceKeyMapping)
	
	/**
	 * A data source factory compatible with product base prices
	 */
	val factoryForBasePrices = factoryWith[ProductBasePrice, ProductBasePriceKeyMapping](ProductBasePriceKeyMapping)
	
	/**
	 * A data source factory compatible with sales groups
	 */
	val factoryForSalesGroups = factoryWith[SalesGroup, SalesGroupKeyMapping](SalesGroupKeyMapping)
	
	
	// OTHER    ----------------------------------
	
	/**
	 * @param mappingFactory A key mapping factory
	 * @tparam A Type of produced item
	 * @tparam M Type of mapping factory
	 * @return A new model to data source parser that uses the specified mapping factory when parsing models
	 */
	def factoryWith[A, M <: KeyMapping[A]](mappingFactory: FromModelFactory[M]): FromModelFactory[DataSource[A]] =
		Factory[A, M](mappingFactory)
	
	
	// NESTED   ----------------------------------
	
	private case class Factory[+A, +M <: KeyMapping[A]](mappingFactory: FromModelFactory[M])
		extends FromModelFactory[DataSource[A]]
	{
		override def apply(model: template.Model[Property]) = schema.validate(model).toTry.flatMap { valid =>
			mappingFactory(valid("mapping").getModel).map { DataSource(valid("path").getString, _,
				valid("header_row_index").getInt, valid("first_data_row_index").intOr(1)) } }
	}
}

/**
 * Describes a file data is read from
 * @author Mikko Hilpinen
 * @since 21.5.2020, v1.1
 * @param filePath Path where the data is read
 * @param mapping Mapping used for interpreting column data and producing output
 * @param headerRowIndex Index of the row that contains the column names. 0 is the first row. Default = 0.
 * @param firstDataRowIndex Index of the first row that contains read data (after header row). 0 is the first row.
 *                          Default = 1.
 */
case class DataSource[+A](filePath: Path, mapping: KeyMapping[A], headerRowIndex: Int = 0,
                          firstDataRowIndex: Int = 1) extends ModelConvertible
{
	override def toModel = Model(Vector("path" -> filePath.toJson, "mapping" -> mapping.toModel,
		"header_row_index" -> headerRowIndex, "first_data_row_index" -> firstDataRowIndex))
}
