package spadi.controller.database.factory.pricing

import spadi.controller.database.Tables
import spadi.model.stored.pricing.{Product, ShopProductInfo}
import utopia.flow.generic.ValueUnwraps._
import utopia.vault.model.immutable.Result
import utopia.vault.nosql.factory.{Deprecatable, FromResultFactory}
import utopia.vault.sql.JoinType
import utopia.vault.util.ErrorHandling

import scala.util.{Failure, Success}

/**
  * Used for reading complete product data from DB
  * @author Mikko Hilpinen
  * @since 1.8.2020, v1.2
  */
object ProductFactory extends FromResultFactory[Product] with Deprecatable
{
	private def nameTable = ProductNameFactory.table
	
	private def netPriceTable = NetPriceFactory.table
	
	private def basePriceTable = BasePriceFactory.table
	
	override def table = Tables.product
	
	override lazy val joinedTables = ProductNameFactory.tables ++ NetPriceFactory.tables ++ BasePriceFactory.tables
	
	override def joinType = JoinType.Left
	
	override def apply(result: Result) =
	{
		// Groups the result by multiple tables
		result.grouped(table, Vector(nameTable, netPriceTable, basePriceTable)).flatMap { case (id, data) =>
			val (myRow, tableRows) = data
			// Validates product row data first
			table.requirementDeclaration.validate(myRow(table)).toTry.map { valid =>
				// Parses linked data, if present
				val names = tableRows.getOrElse(nameTable, Vector()).flatMap(ProductNameFactory.parseIfPresent)
				val netPrices = tableRows.getOrElse(netPriceTable, Vector()).flatMap(NetPriceFactory.parseIfPresent)
				val basePrices = tableRows.getOrElse(basePriceTable, Vector()).flatMap(BasePriceFactory.parseIfPresent)
				// Groups linked data (only preserves one name/netPrice/basePrice per shop)
				val netPricesByShop = netPrices.groupBy { _.shopId }.view.mapValues { _.last }
				val basePricesByShop = basePrices.groupBy { _.shopId }.view.mapValues { _.last }
				val info = names.groupBy { _.shopId }.map { case (shopId, names) => shopId -> ShopProductInfo(names.last,
					basePricesByShop.get(shopId), netPricesByShop.get(shopId)) }
				// Combines all data
				Product(id, valid("electricId"), info)
			} match
			{
				case Success(product) => Some(product)
				case Failure(error) =>
					ErrorHandling.modelParsePrinciple.handle(error)
					None
			}
		}.toVector
	}
	
	override lazy val nonDeprecatedCondition = ProductNameFactory.nonDeprecatedCondition &&
		NetPriceFactory.nonDeprecatedCondition && BasePriceFactory.nonDeprecatedCondition
}
