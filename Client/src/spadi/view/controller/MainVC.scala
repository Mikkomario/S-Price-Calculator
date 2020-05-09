package spadi.view.controller

import spadi.controller.{Log, ReadProducts}
import spadi.model.Product
import spadi.view.component.{ProductsView, SearchField}
import spadi.view.util.Setup._
import utopia.flow.datastructure.mutable.PointerWithEvents
import utopia.flow.util.CombinedOrdering
import utopia.flow.util.StringExtensions._
import utopia.reflection.component.swing.StackableAwtComponentWrapperWrapper
import utopia.reflection.container.swing.{AwtContainerRelated, Stack}
import utopia.reflection.util.Screen
import utopia.reflection.shape.LengthExtensions._

import scala.util.{Failure, Success}

/**
 * The main view controller in the client app
 * @author Mikko Hilpinen
 * @since 9.5.2020, v1
 */
class MainVC extends StackableAwtComponentWrapperWrapper with AwtContainerRelated
{
	// ATTRIBUTES   -----------------------
	
	private implicit val language: String = "fi"
	
	private val producerProductOrdering = Ordering.by[Product, Option[String]] { _.producer }
	private val productNameOrdering = Ordering.by[Product, String] { _.displayName }
	private val productIdOrdering = Ordering.by[Product, String] { _.id }
	private implicit val productOrdering: Ordering[Product] = new CombinedOrdering[Product](
		Vector(producerProductOrdering, productNameOrdering, productIdOrdering))
	
	// Reads and orders product data
	private val allProducts = ReadProducts() match
	{
		case Success(data) =>
			data._2.headOption.foreach { error => Log(error, s"${data._2.size} errors while reading products") }
			data._1.toVector.sorted
		case Failure(error) =>
			Log(error, "Failed to read product data")
			Vector()
	}
	private val productsPointer = new PointerWithEvents[Vector[Product]](allProducts)
	
	// Won't display more than 25 items at once
	private val (searchField, view) = baseContext.inContextWithBackground(colorScheme.primary).use { implicit c =>
		val searchField = SearchField.default("Rajaa tuotteita")
		val productsView = ProductsView(productsPointer.map { _.take(25) }, Screen.height / 2)
		val view = Stack.buildColumnWithContext(isRelated = true) { s =>
			s += searchField
			s += productsView.withAnimatedSize(actorHandler)
		}.framed(margins.medium.any, c.containerBackground)
		
		searchField -> view
	}
	
	
	// INITIAL CODE -----------------------
	
	// Filters products based on search results
	searchField.addValueListener { e =>
		e.newValue match
		{
			case Some(search) =>
				val searchWords = search.words.map { _.toLowerCase }.toSet
				val results = allProducts.map { p => p -> p.matches(searchWords) }
					.filter { _._2 > 0 }.sortBy { -_._2 }
				if (results.nonEmpty)
				{
					// May filter the results further
					val maxMatch = results.head._2
					productsPointer.value = results.takeWhile { _._2 == maxMatch }.map { _._1 }
				}
				else
					productsPointer.value = results.map { _._1 }
			case None => productsPointer.value = allProducts
		}
	}
	
	
	// IMPLEMENTED  -----------------------
	
	override def component = view.component
	
	override protected def wrapped = view
}
