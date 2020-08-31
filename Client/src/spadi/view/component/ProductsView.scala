package spadi.view.component

import spadi.model.stored.pricing.{Product, Shop}
import spadi.view.controller.ProductRowVC
import spadi.view.util.Setup._
import utopia.flow.event.{ChangeEvent, ChangeListener, Changing}
import utopia.flow.util.TimeExtensions.TimeNumber
import utopia.flow.util.WaitUtils
import utopia.reflection.component.context.ColorContext
import utopia.reflection.component.swing.template.StackableAwtComponentWrapperWrapper
import utopia.reflection.container.stack.StackLayout.{Leading, Trailing}
import utopia.reflection.container.swing.layout.SegmentGroup
import utopia.reflection.container.swing.layout.multi.Stack
import utopia.reflection.container.swing.layout.wrapper.scrolling.ScrollView
import utopia.reflection.controller.data.ContainerContentDisplayer
import utopia.reflection.shape.LengthExtensions._
import utopia.reflection.shape.stack.{StackLength, StackLengthLimit}

object ProductsView
{
	/**
	  * @param productsPointer Pointer to displayed products
	  * @param shops Known shops
	  * @param maxOptimalLength Maximum optimal length of the scroll view
	  * @param context Component creation context (implicit)
	  * @return A new products view
	  */
	def apply(productsPointer: Changing[Vector[Product]], shops: Iterable[Shop], maxOptimalLength: Double)
			 (implicit context: ColorContext) = new ProductsView(productsPointer, shops, maxOptimalLength)(context)
}

/**
 * Used for displaying a number of products at once
 * @author Mikko Hilpinen
 * @since 9.5.2020, v1
 */
class ProductsView(productsPointer: Changing[Vector[Product]], shops: Iterable[Shop], maxOptimalLength: Double)
				  (parentContext: ColorContext)
	extends StackableAwtComponentWrapperWrapper
{
	// ATTRIBUTES   -------------------------------
	
	private val stackContext: ColorContext = parentContext.withLightGrayBackground
	
	private val segmentGroup = new SegmentGroup(layouts = Vector(Trailing, Leading, Leading, Leading, Leading, Leading))
	private val stack = stackContext.use { implicit c =>
		val stack = Stack.column[ProductRowVC](margins.medium.any)
		// Registers a content displayer for the stack
		ContainerContentDisplayer.forImmutableStates[Product, ProductRowVC](stack, productsPointer) {
			(a, b) => a.id == b.id } { ProductRowVC(segmentGroup, _, shops) }
		stack
	}
	private val view = parentContext.use { implicit c =>
		val header = ProductRowsHeader(segmentGroup)
		// The stack is placed within a scroll view
		val scrollView = ScrollView.contextual(stack,
			lengthLimits = StackLengthLimit(maxOptimal = Some(maxOptimalLength)))
			.framed(margins.small.any x margins.verySmall.any, stackContext.containerBackground)
		Stack.columnWithItems(Vector(header, scrollView), StackLength.fixedZero)
	}
	
	
	// INITIAL CODE	--------------------------------
	
	// Starts or stops listening to product updates based on whether this view is attached to the main stack hierarchy
	addStackHierarchyChangeListener { isAttached =>
		if (isAttached)
			productsPointer.addListener(ShowDetailsListener)
		else
			productsPointer.removeListener(ShowDetailsListener)
	}
	
	
	// IMPLEMENTED  --------------------------------
	
	override protected def wrapped = view
	
	
	// NESTED	------------------------------------
	
	private object ShowDetailsListener extends ChangeListener[Vector[Product]]
	{
		override def onChangeEvent(event: ChangeEvent[Vector[Product]]) =
		{
			// When only one product is displayed, shows more details about that product (slightly delayed)
			if (event.newValue.size == 1)
				WaitUtils.delayed(0.7.seconds) {
					val components = stack.components
					if (components.size == 1)
						components.head.showDetails()
				}
		}
	}
}
