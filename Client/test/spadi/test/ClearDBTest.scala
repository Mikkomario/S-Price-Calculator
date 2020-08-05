package spadi.test

import spadi.controller.database.{DbSetup, Tables}
import spadi.model.cached.ProgressState
import spadi.view.dialog.LoadingView
import spadi.view.util.Setup
import utopia.flow.async.AsyncExtensions._
import utopia.flow.datastructure.mutable.PointerWithEvents
import utopia.reflection.util.MultiFrameSetup

import scala.util.{Failure, Success}

/**
  * Clears all database data
  * @author Mikko Hilpinen
  * @since 5.8.2020, v1.2
  */
object ClearDBTest extends App
{
	import Setup._
	
	implicit val languageCode: String = "en"
	
	val setup = new MultiFrameSetup(actorHandler)
	setup.start()
	
	val progressPointer = new PointerWithEvents(ProgressState.initial("Setting up the database"))
	val loadCompletion = new LoadingView(progressPointer).display()
	
	DbSetup.setup(progressPointer)
	
	connectionPool.tryWith { implicit connection =>
		connection.dropDatabase(Tables.databaseName)
	} match
	{
		case Success(_) => println("Database successfully dropped")
		case Failure(error) => error.printStackTrace()
	}
	
	loadCompletion.waitFor()
	println("Done")
}
