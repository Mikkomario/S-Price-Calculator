package spadi.view.controller

import java.time.Instant

import spadi.controller.Log
import spadi.controller.database.access.id.ProductIds
import spadi.controller.database.access.multi.DbProducts
import spadi.model.stored.pricing.{Product, Shop}
import spadi.view.component.{MainViewHeader, Overview, ProductsView, SearchField}
import spadi.view.util.Icons
import spadi.view.util.Setup._
import utopia.flow.async.Volatile
import utopia.flow.datastructure.mutable.PointerWithEvents
import utopia.flow.util.StringExtensions._
import utopia.flow.util.WaitUtils
import utopia.flow.util.TimeExtensions._
import utopia.flow.util.CollectionExtensions._
import utopia.genesis.util.Screen
import utopia.reflection.color.ColorRole.Info
import utopia.reflection.component.swing.label.{ImageLabel, TextLabel}
import utopia.reflection.component.swing.template.StackableAwtComponentWrapperWrapper
import utopia.reflection.container.stack.StackLayout.Center
import utopia.reflection.container.swing.AwtContainerRelated
import utopia.reflection.container.swing.layout.multi.Stack
import utopia.reflection.container.swing.layout.multi.Stack.AwtStackable
import utopia.reflection.container.swing.layout.wrapper.SwitchPanel
import utopia.reflection.shape.LengthExtensions._
import utopia.reflection.shape.stack.StackLength
import utopia.reflection.localization.LocalString._

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
	private val context = baseContext.inContextWithBackground(colorScheme.primary)
	
	private val minSearchDelay = 0.25.seconds
	
	private var lastSearchTime = Instant.now()
	private val currentSearchCompletion = Volatile(Future.successful(()))
	
	private val productsPointer = new PointerWithEvents[Vector[Product]](defaultProducts)
	private val (productsView, startView, contentPanel) = context.use { implicit c =>
		val productsView = ProductsView(productsPointer, shops, Screen.height / 2)
		val startView = Overview()
		val contentPanel = SwitchPanel[AwtStackable](startView)
		
		(productsView, startView, contentPanel)
	}
	private val (noResultsView, noResultsLabel) = context.forChildComponentWithRole(Info)
		.forTextComponents().expandingToRight
		.use { implicit c =>
			val textLabel = TextLabel.contextual()
			Stack.buildRowWithContext(layout = Center, isRelated = true) { s =>
				s += ImageLabel.contextual(Icons.info.singleColorImage)
				s += textLabel
			}.framed(margins.small.any, c.containerBackground) -> textLabel
		}
	
	// Won't display more than 100 items at once
	private val (searchField, view) = context.use { implicit c =>
		val searchField = SearchField.default("Etsi tuotteita")
		val mainView = Stack.buildColumnWithContext(isRelated = true) { s =>
			s += searchField
			s += contentPanel.withAnimatedSize
		}.framed(margins.medium.any, c.containerBackground)
		val stack = Stack.columnWithItems(Vector(new MainViewHeader(shops), mainView), StackLength.fixedZero)
		
		searchField -> stack
	}
	
	
	// INITIAL CODE -----------------------
	
	// Focus is initially set to the search field
	onNextStackHierarchyAttachment { searchField.requestFocusInWindow() }
	
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
				
				lastSearchTime = Instant.now()
				// Completes current search
				newPromise.success(())
				
				// After wait, checks current search words
				val currentSearchWords = searchField.text.words
				if (currentSearchWords.isEmpty)
					contentPanel.set(startView)
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
						//println(s"Top product: ${sortedProducts.headOption}")
						productsPointer.value = sortedProducts
						if (sortedProducts.isEmpty)
						{
							noResultsLabel.text = "Ei yhtään tuotetta haulla: %s".autoLocalized
								.interpolated(Vector(currentSearchWords.mkString(", ")))
							contentPanel.set(noResultsView)
						}
						else
							contentPanel.set(productsView)
					}.failure.foreach { error => Log(error, "Failed to search for products") }
				}
			}
		}
	}
	
	
	// IMPLEMENTED  -----------------------
	
	override def component = view.component
	
	override protected def wrapped = view
}
