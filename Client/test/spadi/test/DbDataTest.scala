package spadi.test

import spadi.controller.database.{DbSetup, Tables}
import spadi.model.cached.ProgressState
import utopia.flow.datastructure.mutable.PointerWithEvents
import utopia.vault.sql.{Limit, SelectAll}

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
		// Reads a few rows from each table
		Tables.all.foreach { table =>
			println(s"\nData for ${table.name}\nExpecting ${table.columns.map { _.name }.mkString(", ")}")
			println(connection(SelectAll(table) + Limit(3)))
		}
	}
	
	println("\nDone")
	System.exit(0)
}
