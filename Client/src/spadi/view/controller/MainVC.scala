package spadi.view.controller

import java.time.Instant

import spadi.controller.Log
import spadi.controller.database.access.id.ProductIds
import spadi.controller.database.access.multi.DbProducts
import spadi.model.stored.pricing.{Product, Shop}
import spadi.view.component.{MainViewHeader, ProductsView, SearchField}
import spadi.view.util.Setup._
import utopia.flow.async.Volatile
import utopia.flow.datastructure.mutable.PointerWithEvents
import utopia.flow.util.StringExtensions._
import utopia.flow.util.WaitUtils
import utopia.flow.util.TimeExtensions._
import utopia.flow.util.CollectionExtensions._
import utopia.genesis.util.Screen
import utopia.reflection.component.swing.template.StackableAwtComponentWrapperWrapper
import utopia.reflection.container.swing.AwtContainerRelated
import utopia.reflection.container.swing.layout.multi.Stack
import utopia.reflection.shape.LengthExtensions._
import utopia.reflection.shape.StackLength

import scala.concurrent.{Future, Promise}

/**
 * The main view controller in the client app
 * @author Mikko Hilpinen
 * @since 9.5.2020, v1
 */
class MainVC(shops: Iterable[Shop], defaultProducts: Vector[Product])
	extends StackableAwtComponentWrapperWrapper with AwtContainerRelated
{
	// ATTRIBUTES   -----------------------
	
	private implicit val language: String = "fi"
	
	private val minSearchDelay = 0.2.seconds
	
	private var lastSearchTime = Instant.now()
	private val currentSearchCompletion = Volatile(Future.successful(()))
	
	//private val producerProductOrdering = Ordering.by[Product, Option[String]] { _.producer }
	//private val productNameOrdering = Ordering.by[Product, String] { _.displayName }
	// private implicit val productIdOrdering: Ordering[Product] = Ordering.by[Product, String] { _.id }
	//private implicit val productOrdering: Ordering[Product] = new CombinedOrdering[Product](
	//	Vector(producerProductOrdering, productNameOrdering, productIdOrdering))
	
	// Reads and orders product data
	/*
	private val allProducts = ReadProducts() match
	{
		case Success(data) =>
			data._2.headOption.foreach { error => Log(error, s"${data._2.size} errors while reading products") }
			data._1.toVector.sorted
		case Failure(error) =>
			Log(error, "Failed to read product data")
			Vector()
	}*/
	private val productsPointer = new PointerWithEvents[Vector[Product]](defaultProducts)
	
	// Won't display more than 100 items at once
	private val (searchField, view) = baseContext.inContextWithBackground(colorScheme.primary).use { implicit c =>
		val searchField = SearchField.default("Rajaa tuotteita")
		val productsView = ProductsView(productsPointer, shops, Screen.height / 2)
		val mainView = Stack.buildColumnWithContext(isRelated = true) { s =>
			s += searchField
			s += productsView.withAnimatedSize(actorHandler)
		}.framed(margins.medium.any, c.containerBackground)
		val stack = Stack.columnWithItems(Vector(new MainViewHeader(shops), mainView), StackLength.fixedZero)
		
		searchField -> stack
	}
	
	
	// INITIAL CODE -----------------------
	
	// Focus is initially set to the search field
	searchField.requestFocusInWindow()
	
	// Filters products based on search results
	searchField.addValueListener { _ =>
		lastSearchTime = Instant.now()
		
		// Only updates search on a delay (one background process active at a time)
		currentSearchCompletion.pop { current =>
			if (current.isCompleted)
			{
				val newPromise = Promise[Unit]()
				Some(newPromise) -> newPromise.future
			}
			else
				None -> current
		}.foreach { newPromise =>
			Future {
				// Waits until enough time has passed since the last search update
				val waitLock = new AnyRef
				while (Instant.now() < lastSearchTime + minSearchDelay)
					WaitUtils.waitUntil(lastSearchTime + minSearchDelay, waitLock)
				
				// After wait, checks current search words
				val currentSearchWords = searchField.text.words
				if (currentSearchWords.isEmpty)
					productsPointer.value = defaultProducts
				else
				{
					// Performs the search
					connectionPool.tryWith { implicit connection =>
						val productIds = ProductIds.forProductsMatching(currentSearchWords.toSet, 50)
						val products = if (productIds.nonEmpty) DbProducts.withIds(productIds) else Vector()
						// Sorts the products based on search match level
						val sortedProducts = products.sortBy { _.electricId }.sortBy { p =>
							val electricIdMatchLevel = currentSearchWords.count(p.electricId.containsIgnoreCase)
							val productNameMatchLevel = currentSearchWords.count { word =>
								p.shopData.exists { _.name.containsIgnoreCase(word) } }
							val altNameMatchLevel = currentSearchWords.count { word =>
								p.shopData.exists { _.alternativeName.exists { _.containsIgnoreCase(word) } } }
							
							electricIdMatchLevel * -10 - productNameMatchLevel * 2 - altNameMatchLevel
						}
						// println(s"Top product: ${sortedProducts.headOption}")
						productsPointer.value = sortedProducts
					}.failure.foreach { error => Log(error, "Failed to search for products") }
				}
				
				// Completes current search
				newPromise.success(())
			}
		}
	}
	
	
	// IMPLEMENTED  -----------------------
	
	override def component = view.component
	
	override protected def wrapped = view
}
