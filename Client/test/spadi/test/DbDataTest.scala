package spadi.test

import spadi.controller.AnalyzeProducts
import spadi.controller.database.access.multi.DbShopProducts
import spadi.controller.database.factory.pricing.ProductFactory
import spadi.controller.database.model.pricing.ProductModel
import spadi.controller.database.{DbSetup, Tables}
import spadi.model.cached.ProgressState
import utopia.flow.datastructure.mutable.PointerWithEvents
import utopia.vault.sql.{Limit, SelectAll, Where}

/**
  * Reads some data from the database
  * @author Mikko Hilpinen
  * @since 8.8.2020, v1.2
  */
object DbDataTest extends App
{
	import spadi.view.util.Setup._
	
	implicit val languageCode: String = "fi"
	
	val dbProgressPointer = new PointerWithEvents(ProgressState.initial("Setting up database"))
	dbProgressPointer.addListener { e => println(e.newValue) }
	DbSetup.setup()
	
	connectionPool { implicit connection =>
		// Lists all tables first
		println("Tables:")
		connection.executeQuery("SHOW TABLES").foreach { println(_) }
		
		// Reads a few rows from each table
		Tables.all.foreach { table =>
			println(s"\nData for ${table.name}\nExpecting ${table.columns.map { _.name }.mkString(", ")}")
			println(connection(SelectAll(table) + Limit(3)))
		}
		
		// Reads a few products for each shop
		/*
		DbShops.all.foreach { shop =>
			println(s"\nData for ${shop.name}")
			DbShopProducts.forShopWithId(shop.id).take(5).foreach { println(_) }
		}*/
		
		/*
		println()
		println(connection(SelectAll(ShopProductFactory.target) +
			Where(ShopProductModel.withId(254709).toCondition && ShopProductFactory.nonDeprecatedCondition)))
		 */
		
		println()
		println(connection(SelectAll(ProductFactory.target) + Where(ProductModel.withId(58739).toCondition)))
		
		println()
		DbShopProducts.count.foreach { case (shop, count) => println(s"Products in ${shop.name} (${shop.id}): $count") }
		
		println()
		println("Analyzing products")
		val analysisProgress = new PointerWithEvents(ProgressState.initial("Preparing analysis"))
		analysisProgress.addListener { e => println(e.newValue) }
		AnalyzeProducts(analysisProgress).foreach { case (shopId, report) => println(s"$shopId => $report") }
	}
	
	println("\nDone")
	System.exit(0)
}
