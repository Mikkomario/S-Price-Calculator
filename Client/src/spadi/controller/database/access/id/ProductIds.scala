package spadi.controller.database.access.id

import spadi.controller.database.factory.pricing.{ProductFactory, ShopProductFactory}
import utopia.flow.generic.ValueConversions._
import utopia.flow.util.CollectionExtensions._
import utopia.vault.database.Connection
import utopia.vault.sql.{Limit, Select, SelectDistinct, Where}
import utopia.vault.sql.Extensions._

/**
  * Used for accessing product ids
  * @author Mikko Hilpinen
  * @since 5.8.2020, v1.2
  */
object ProductIds
{
	// COMPUTED	--------------------------
	
	private def table = factory.table
	
	private def column = table.primaryColumn.get
	
	private def factory = ProductFactory
	
	private def shopProductFactory = ShopProductFactory
	
	private def electricIdColumn = factory.electricIdColumn
	
	
	// OTHER	--------------------------
	
	/**
	  * Finds product ids that match electric ids
	  * @param min First included electric id
	  * @param max Last included electric id
	  * @param connection DB Connection (implicit)
	  * @return An electric id -> product id map containing all recorded electric ids within range
	  */
	def forElectricIdsBetween(min: String, max: String)(implicit connection: Connection) =
	{
		connection(Select(table, Vector(column, electricIdColumn)) +
			Where(electricIdColumn.isBetween(min, max)))
			.rows.map { row => row(electricIdColumn).getString -> row(column).getInt }.toMap
	}
	
	/**
	  * @param min First targeted product id
	  * @param max Last targeted product id
	  * @param connection DB Connection (implicit)
	  * @return Electric id to product id map for that product id range
	  */
	def electricIdMapForProductIdsBetween(min: Int, max: Int)(implicit connection: Connection) =
	{
		connection(Select(table, Vector(column, electricIdColumn)) + Where(column.isBetween(min, max)))
			.rows.map { row => row(electricIdColumn).getString -> row(column).getInt }.toMap
	}
	
	/**
	  * Finds ids of products based on product electric id match
	  * @param electricIdFilter Electric id filter
	  * @param connection DB Connection (implicit)
	  * @return Products that have an electric id that resembles specified filter
	  */
	def forProductsWithMatchingElectricId(electricIdFilter: String)(implicit connection: Connection) =
	{
		connection(SelectDistinct(factory.table, column) +
			Where(factory.electricIdColumn.contains(electricIdFilter))).rowIntValues
	}
	
	/**
	  * Finds ids of the products that match specified search keys
	  * @param searchFilters Search keys used
	  * @param maxResultSize Maximum number of ids returned (default = 100)
	  * @param connection DB Connection (implicit)
	  * @return Product ids that match specified search keys
	  */
	// TODO: Add better product filtering (products matching multiple filter should be prioritized)
	def forProductsMatching(searchFilters: Set[String], maxResultSize: Int = 100)(implicit connection: Connection) =
	{
		// Considers all digit-containing search words to be electric ids
		val (nameFilters, electricIdFilters) = searchFilters.divideBy { _.exists { _.isDigit } }
		val nameConditions = nameFilters.map { filter => shopProductFactory.nameColumn.contains(filter) ||
			shopProductFactory.alternativeNameColumn.contains(filter) }.toVector
		val electricIdConditions = electricIdFilters.map { filter => factory.electricIdColumn.contains(filter) }.toVector
		
		if (nameConditions.nonEmpty)
		{
			val nameCondition = nameConditions.head || nameConditions.tail
			if (electricIdConditions.nonEmpty)
			{
				// When both name and search key conditions are given, tries first by applying one or more of both
				val target = shopProductFactory.table join factory.table
				val electricIdCondition = electricIdConditions.head || electricIdConditions.tail
				
				val combinationResults = connection(SelectDistinct(target, column) +
					Where(electricIdCondition && nameCondition) + Limit(maxResultSize)).rowIntValues
				if (combinationResults.nonEmpty)
					combinationResults
				else
				{
					// If no results were found, searches with electric ids only
					val onlyElectricIdResults = connection(SelectDistinct(table, column) + Where(electricIdCondition) +
						Limit(maxResultSize)).rowIntValues
					if (onlyElectricIdResults.nonEmpty)
						onlyElectricIdResults
					else
					{
						// If no results were still found, searches with product names only
						// (inserting electric ids to possible product names as well)
						val electricIdsInNamesCondition = electricIdFilters.map { filter =>
							shopProductFactory.nameColumn.contains(filter) ||
							shopProductFactory.alternativeNameColumn.contains(filter) }.toVector
						connection(SelectDistinct(shopProductFactory.table, shopProductFactory.productIdColumn) +
							Where(nameCondition || electricIdsInNamesCondition) + Limit(maxResultSize)).rowIntValues
					}
				}
			}
			else
				connection(SelectDistinct(shopProductFactory.table, shopProductFactory.productIdColumn) +
					Where(nameCondition) + Limit(maxResultSize)).rowIntValues
		}
		else if (electricIdConditions.nonEmpty)
		{
			val electricIdCondition = electricIdConditions.head || electricIdConditions.tail
			connection(SelectDistinct(table, column) + Where(electricIdCondition) +
				Limit(maxResultSize)).rowIntValues
		}
		else
			Vector()
	}
}
