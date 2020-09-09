package spadi.controller

import spadi.controller.container.LocalContainer
import utopia.flow.datastructure.immutable.Value
import utopia.flow.generic.ValueConversions._
import utopia.genesis.shape.shape2D.Point
import utopia.genesis.generic.GenesisValue._
import utopia.genesis.shape.path.BezierFunction

import scala.util.Success

/**
 * Function used for calculating profits percentage for each product
 * @author Mikko Hilpinen
 * @since 9.5.2020, v1
 */
object ProfitsPercentage
{
	// ATTRIBUTES   ----------------------------
	
	private val defaultPoints = Vector(Point(0, 23), Point(20, 20), Point(100, 10), Point(200, 5), Point(1000, 2),
		Point(10000, 1))
	private val function =
	{
		val points =
		{
			// Requires at least 3 points to form a function
			val readPoints = PointsContainer.current
			if (readPoints.size < 3)
			{
				PointsContainer.current = defaultPoints
				defaultPoints
			}
			else
				readPoints
		}
		BezierFunction(points)
	}
	
	
	// OTHER    --------------------------------
	
	/**
	 * @param price Original price
	 * @return Profits percentage that should be used with that price
	 */
	def forPrice(price: Double) = function(price)
	
	
	// NESTED   --------------------------------
	
	private object PointsContainer extends LocalContainer[Vector[Point]]("profits-function.json")
	{
		override protected def toJsonValue(item: Vector[Point]) = item
		
		override protected def fromJsonValue(value: Value) = Success(value.getVector.flatMap { _.point })
		
		override protected def empty = Vector()
	}
}
