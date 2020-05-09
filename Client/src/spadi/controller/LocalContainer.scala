package spadi.controller

import spadi.controller.Globals._
import utopia.flow.async.Volatile
import utopia.flow.datastructure.immutable.Value
import utopia.flow.parse.JSONReader
import utopia.flow.util.CollectionExtensions._
import utopia.flow.util.FileExtensions._

import scala.concurrent.{Future, Promise}
import scala.util.{Failure, Success, Try}

/**
 * Common trait for data storabes that back themselves up in a local file
 * @author Mikko Hilpinen
 * @since 8.5.2020, v1
 */
abstract class LocalContainer[A](fileName: String)
{
	// ABSTRACT	-------------------------------
	
	/**
	 * @param item An item to convert into a json value
	 * @return Value based on the item
	 */
	protected def toJsonValue(item: A): Value
	
	/**
	 * @return function for converting read json value to an item
	 */
	protected def fromJsonValue(value: Value): Try[A]
	
	/**
	 * @return An empty item
	 */
	protected def empty: A
	
	
	// ATTRIBUTES	---------------------------
	
	private lazy val _current = new Volatile(fromFile)
	private val saveCompletion = Volatile(Future.successful(()))
	
	
	// COMPUTED	-------------------------------
	
	/**
	 * @return Location where this container's data is backed up
	 */
	val fileLocation = dataDirectory/fileName
	
	/**
	 * @return The currently stored data
	 */
	def current = _current.get
	def current_=(newPrices: A) =
	{
		_current.set(newPrices)
		saveStatus()
	}
	
	
	// OTHER	-------------------------------
	
	private def saveStatus() =
	{
		// Will only perform one saving at a time
		val newSavePromise = Promise[Unit]()
		saveCompletion.getAndSet(newSavePromise.future).onComplete { _ =>
			// Saves current status to file as json
			val dataToSave = toJsonValue(_current.get)
			fileLocation.createParentDirectories()
			fileLocation.writeJSON(dataToSave).failure.foreach { error =>
				Log(error, s"Failed to save data to $fileLocation")
			}
			// Completes the promise so that the next save process can start
			newSavePromise.success(())
		}
	}
	
	private def fromFile =
	{
		if (fileLocation.exists)
		{
			JSONReader(fileLocation.toFile) match
			{
				case Success(value) =>
					fromJsonValue(value) match
					{
						case Success(item) => item
						case Failure(error) =>
							Log(error, s"Failed to read stored data from $fileLocation")
							empty
					}
				case Failure(error) =>
					Log(error, s"Failed to read stop list file from $fileLocation")
					empty
			}
		}
		else
			empty
	}
}
