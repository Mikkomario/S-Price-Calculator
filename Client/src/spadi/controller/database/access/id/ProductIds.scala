package spadi.controller.database.access.id

import spadi.controller.database.factory.pricing.{ProductFactory, ShopProductFactory}
import utopia.flow.util.CollectionExtensions._
import utopia.vault.database.Connection
import utopia.vault.sql.{Limit, SelectDistinct, Where}

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
	
	
	// OTHER	--------------------------
	
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
		val (nameFilters, electricIdFilters) = searchFilters.divideBy { _.forall { _.isDigit } }
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
						connection(SelectDistinct(shopProductFactory.table, shopProductFactory.productIdColumn) +
							Where(nameCondition) + Limit(maxResultSize)).rowIntValues
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
