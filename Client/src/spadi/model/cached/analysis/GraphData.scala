package spadi.model.cached.analysis

/**
  * Contains a set of points that form a continuous line / graph
  * @author Mikko Hilpinen
  * @since 14.9.2020, v1.2.3
  */
case class GraphData[+X, +Y](points: Vector[(X, Y)])
{
	/**
	  * @param x An x-coordinate
	  * @return The last point in this graph before the specified coordinate (inclusive)
	  */
	def apply[B >: X](x: B)(implicit ordering: Ordering[B]) = points.findLast { case (x2, _) =>
		ordering.compare(x2, x) <= 0 }.map { _._2 }
	
	def +[X2 >: X, Y2 >: Y](other: GraphData[X2, Y2])(implicit ordering: Ordering[X2]) =
	{
		// TODO: Implement
	}
}
