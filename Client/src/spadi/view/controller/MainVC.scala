package spadi.view.controller

import spadi.controller.{Log, ReadProducts}
import spadi.model.Product
import spadi.view.component.ProductsView
import spadi.view.util.Setup._
import utopia.flow.datastructure.mutable.PointerWithEvents
import utopia.flow.util.CombinedOrdering
import utopia.reflection.component.swing.StackableAwtComponentWrapperWrapper
import utopia.reflection.container.swing.AwtContainerRelated
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
	
	private val productsPointer = new PointerWithEvents[Vector[Product]](Vector())
	private val view = baseContext.inContextWithBackground(colorScheme.primary).use { implicit c =>
		ProductsView(productsPointer, Screen.height / 2).framed(margins.medium.any, c.containerBackground)
	}
	
	private val producerProductOrdering = Ordering.by[Product, Option[String]] { _.producer }
	private val productNameOrdering = Ordering.by[Product, String] { _.displayName }
	private val productIdOrdering = Ordering.by[Product, String] { _.id }
	private implicit val productOrdering: Ordering[Product] = new CombinedOrdering[Product](
		Vector(producerProductOrdering, productNameOrdering, productIdOrdering))
	
	
	// INITIAL CODE -----------------------
	
	// Reads and orders product data
	ReadProducts() match
	{
		case Success(data) =>
			data._2.headOption.foreach { error => Log(error, s"${data._2.size} errors while reading products") }
			productsPointer.value = data._1.toVector.sorted.take(100) // Limits to 100 items (TODO: Add better handling)
		case Failure(error) => Log(error, "Failed to read product data")
	}
	
	
	// IMPLEMENTED  -----------------------
	
	override def component = view.component
	
	override protected def wrapped = view
}
