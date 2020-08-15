package spadi.controller.database.access.id

import spadi.controller.database.factory.pricing.{ProductFactory, ShopProductFactory}
import utopia.flow.generic.ValueConversions._
import utopia.flow.util.CollectionExtensions._
import utopia.vault.database.Connection
import utopia.vault.sql.{Condition, Limit, Select, SelectDistinct, Where}
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
		
		// Performs the first search using both name- and electric search conditions, if available
		// (applied only partially if necessary)
		val primaryResults = searchWithNames(nameFilters, electricIdFilters, maxResultSize)
		if (primaryResults.nonEmpty)
			primaryResults
		else
		{
			// If no results were found, applies only electric id condition
			// (provided that it wasn't already done in the primary test)
			val electricIdOnlyResults =
				if (nameFilters.nonEmpty) searchWithElectricIds(electricIdFilters, maxResultSize) else Vector()
			if (electricIdOnlyResults.nonEmpty)
				electricIdOnlyResults
			else
			{
				// If there still aren't results, adds the specified electric id search words to the pool of
				// searched names and tries again. Skips if this would result in the same name search pool anyway.
				if (electricIdFilters.nonEmpty)
					searchWithNames(nameFilters ++ electricIdFilters, Vector(), maxResultSize)
				else
					Vector()
			}
		}
	}
	
	private def searchWithElectricIds(electricIdWords: Iterable[String], maxResultsSize: Int)
									(implicit connection: Connection) =
	{
		electricIdSearchCondition(electricIdWords) match
		{
			case Some(condition) =>
				connection(SelectDistinct(table, column) + Where(condition) + Limit(maxResultsSize)).rowIntValues
			case None => Vector()
		}
	}
	
	private def searchWithNames(nameWords: Iterable[String], electricIdWords: Iterable[String], maxResultsSize: Int)
							   (implicit connection: Connection) =
	{
		nameSearchCondition(nameWords) match
		{
			case Some(nameCondition) =>
				val condition = electricIdSearchCondition(electricIdWords) match
				{
					case Some(electricIdCondition) => nameCondition && electricIdCondition
					case None => nameCondition
				}
				connection(SelectDistinct(shopProductFactory.table join factory.table, column) +
					Where(condition) + Limit(maxResultsSize)).rowIntValues
				
			case None => searchWithElectricIds(electricIdWords, maxResultsSize)
		}
	}
	
	private def nameSearchCondition(searchWords: Iterable[String]) =
		combineSearchConditions(nameSearchConditions(searchWords).toVector)
	
	private def electricIdSearchCondition(searchWords: Iterable[String]) =
		combineSearchConditions(electricIdSearchConditions(searchWords).toVector)
	
	private def combineSearchConditions(conditions: Seq[Condition]) = conditions.headOption match
	{
		case Some(first) => Some(first || conditions.tail)
		case None => None
	}
	
	private def nameSearchConditions(searchWords: Iterable[String]) =
	{
		searchWords.map { filter => shopProductFactory.nameColumn.contains(filter) ||
			shopProductFactory.alternativeNameColumn.contains(filter) }
	}
	
	private def electricIdSearchConditions(searchWords: Iterable[String]) = searchWords.map { filter =>
		factory.electricIdColumn.contains(filter) }
}
