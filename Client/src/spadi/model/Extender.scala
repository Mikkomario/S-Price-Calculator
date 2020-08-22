package spadi.model

import scala.language.implicitConversions

object Extender
{
	implicit def autoUnwrap[W](e: Extender[W]): W = e.wrapped
}

/**
  * A common trait for enriched / extended items
  * @author Mikko Hilpinen
  * @since 16.8.2020, v1.2.1
  */
trait Extender[+Wrapped]
{
	/**
	  * @return The wrapped item
	  */
	def wrapped: Wrapped
}
