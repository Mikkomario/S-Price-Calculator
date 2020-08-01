package spadi.controller.database

import spadi.controller.Globals._

/**
  * Used for accessing database tables for S-Price project
  * @author Mikko Hilpinen
  * @since 28.7.2020, v1.2
  */
object Tables
{
	// ATTRIBUTES	---------------------------
	
	/**
	  * Name of the database that contains these tables
	  */
	val databaseName = "s_price"
	
	/**
	  * Name of the table that contains database version recordings
	  */
	val versionTableName = "database_version"
	
	private val tables = new utopia.vault.database.Tables(connectionPool)
	
	
	// COMPUTED	-------------------------------
	
	/**
	  * @return Table that contains database version recordings
	  */
	def databaseVersion = apply(versionTableName)
	
	/**
	  * @return Table that contains all shops
	  */
	def shop = apply("shop")
	
	/**
	  * @return Table that contains all shop sale groups
	  */
	def saleGroup = apply("sale_group")
	
	/**
	  * @return Table that contains current & historical amounts of each sale group
	  */
	def saleAmount = apply("sale_amount")
	
	/**
	  * @return Table that lists all products
	  */
	def product = apply("product")
	
	/**
	  * @return Table that contains the names of all products in each shop
	  */
	def productName = apply("shop_product_name")
	
	/**
	  * @return Table that contains product net prices in different shops
	  */
	def netPrice = apply("shop_product_net_price")
	
	/**
	  * @return Table that contains product base prices in different shops
	  */
	def basePrice = apply("shop_product_base_price")
	
	/**
	  * @return Table that contains parse instructions for shop net price documents
	  */
	def netPriceKeyMap = apply("net_price_key_map")
	
	/**
	  * @return Table that contains read instructions for shop base price documents
	  */
	def basePriceKeyMap = apply("base_price_key_map")
	
	/**
	  * @return Table that contains read instructions for sale group documents
	  */
	def saleGroupKeyMap = apply("sale_group_key_map")
	
	
	// OTHER	-------------------------------
	
	private def apply(tableName: String) = tables(databaseName, tableName)
}
