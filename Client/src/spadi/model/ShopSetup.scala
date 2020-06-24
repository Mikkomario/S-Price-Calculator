package spadi.model

import utopia.flow.datastructure.immutable.{Model, ModelDeclaration, PropertyDeclaration, Value}
import utopia.flow.datastructure.template
import utopia.flow.datastructure.template.Property
import utopia.flow.generic.{FromModelFactory, ModelConvertible, ModelType}
import utopia.flow.generic.ValueConversions._
import utopia.flow.util.CollectionExtensions._

object ShopSetup extends FromModelFactory[ShopSetup]
{
	// ATTRIBUTES   -----------------------------
	
	private val schema = ModelDeclaration(PropertyDeclaration("shop", ModelType))
	
	
	// IMPLEMENTED  -----------------------------
	
	override def apply(model: template.Model[Property]) = schema.validate(model).toTry.flatMap { valid =>
		Shop(valid("shop").getModel).flatMap { shop =>
			// Either price source or base source + sale source must be present and parseable
			valid("price_source").model match
			{
				case Some(priceSourceModel) => DataSource.factoryForProductPrices(priceSourceModel).map { priceSource =>
					ShopSetup(shop, Right(priceSource)) }
				case None =>
					valid("base_source").model.flatMap { bs => valid("sale_source").model.map { bs -> _ } }.toTry {
						new NoSuchElementException("Either 'price_source' or 'base_source' and 'sale_source' is required") }
						.flatMap { case (baseSourceModel, saleSourceModel) =>
							DataSource.factoryForBasePrices(baseSourceModel).flatMap { baseSource =>
								DataSource.factoryForSalesGroups(saleSourceModel).map { saleSource =>
									ShopSetup(shop, Left(baseSource -> saleSource))
								}
							}
						}
			}
		}
	}
}

/**
 * Contains information required for reading shop data
 * @author Mikko Hilpinen
 * @since 21.5.2020, v1.1
 */
case class ShopSetup(shop: Shop, dataSource: Either[(DataSource[ProductBasePrice], DataSource[SalesGroup]),
	DataSource[ProductPrice]]) extends ModelConvertible
{
	// COMPUTED ---------------------------
	
	/**
	 * @return Paths read by in setup
	 */
	def paths = dataSource match
	{
		case Right(comboSource) => Vector(comboSource.filePath)
		case Left((baseSource, saleSource)) => Vector(baseSource.filePath, saleSource.filePath)
	}
	
	
	// IMPLEMENTED  -----------------------
	
	override def toModel =
	{
		val dataSourceProperties: Vector[(String, Value)] = dataSource match
		{
			case Right(fullSource) => Vector("price_source" -> fullSource.toModel)
			case Left((baseSource, saleSource)) =>
				Vector("base_source" -> baseSource.toModel, "sale_source" -> saleSource.toModel)
		}
		val allProperties: Vector[(String, Value)] = ("shop" -> (shop.toModel: Value)) +: dataSourceProperties
		Model(allProperties)
	}
	
	
	// OTHER    --------------------------
	
	/**
	 * Maps the data source of this setup if this setup uses a single data source
	 * @param f Mapping function
	 * @return Mapped setup
	 */
	def mapDataSourceIfCombo(f: DataSource[ProductPrice] => DataSource[ProductPrice]) = dataSource match {
		case Right(combo) => copy(dataSource = Right(f(combo)))
		case Left(_) => this
	}
	
	/**
	 * Maps the data sources of this seutp if this setup uses two data sources
	 * @param f Mapping function
	 * @return Mapped setup
	 */
	def mapDataSourceIfSplit(
		f: (DataSource[ProductBasePrice], DataSource[SalesGroup]) => (DataSource[ProductBasePrice], DataSource[SalesGroup])) =
	{
		dataSource match
		{
			case Right(_) => this
			case Left((price, sale)) => copy(dataSource = Left(f(price, sale)))
		}
	}
}
