package spadi.controller.container

import utopia.flow.datastructure.immutable.Value
import utopia.flow.generic.ValueConversions._
import utopia.flow.generic.{FromModelFactory, ModelConvertible}
import utopia.flow.util.CollectionExtensions._

import scala.util.{Failure, Success}

/**
 * These containers store lists of data
 * @author Mikko Hilpinen
 * @since 8.5.2020, v1
 */
class LocalModelsContainer[A <: ModelConvertible](fileName: String, factory: FromModelFactory[A]) extends LocalContainer[Vector[A]](fileName)
{
	override protected def toJsonValue(item: Vector[A]) = item.map { _.toModel }
	
	override protected def fromJsonValue(value: Value) =
	{
		// All reads must not fail
		val (failures, successes) = value.getVector.flatMap { _.model }.map { factory(_) }.divideBy { _.isSuccess }
		if (successes.isEmpty && failures.nonEmpty)
			Failure(failures.head.failure.get)
		else
			Success(successes.map { _.get })
	}
	
	override protected def empty = Vector()
}
