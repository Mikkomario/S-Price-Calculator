package spadi.view.component

import spadi.model.stored.pricing.Shop
import spadi.view.dialog.DeleteShopWindow
import spadi.view.util.Icons
import utopia.reflection.component.swing.template.StackableAwtComponentWrapperWrapper
import spadi.view.util.Setup._
import utopia.genesis.shape.shape2D.Point
import utopia.reflection.component.swing.button.{ImageAndTextButton, ImageButton}
import utopia.reflection.component.swing.label.TextLabel
import utopia.reflection.container.swing.layout.multi.Stack
import utopia.reflection.container.swing.window.Popup
import utopia.reflection.shape.Alignment.TopRight
import utopia.reflection.shape.LengthExtensions._

/**
  * A header component for the main view
  * @author Mikko Hilpinen
  * @since 15.8.2020, v1.2
  */
class MainViewHeader(initialShops: Iterable[Shop]) extends StackableAwtComponentWrapperWrapper
{
	// ATTRIBUTES	-----------------------
	
	private implicit val languageCode: String = "fi"
	
	private val color = primaryColors.dark
	private val context = baseContext.inContextWithBackground(color)
	
	private var shops = initialShops.toSet
	
	private val menuButton = context.use { implicit c =>
		ImageButton.contextual(Icons.menu.asIndividualButton) { displayMenu() }
	}
	private val titleLabel = context.forTextComponents().use { implicit c => TextLabel.contextual("S-Price") }
	private val view = Stack.rowWithItems(Vector(titleLabel, menuButton), margins.small.upscaling.expanding)
		.framed(margins.small.downscaling, color)
	
	
	// IMPLEMENTED	-----------------------
	
	override protected def wrapped = view
	
	
	// OTHER	---------------------------
	
	private def displayMenu(): Unit =
	{
		val deleteShopButton = baseContext.inContextWithBackground(color).forTextComponents()
			.forCustomColorButtons(colorScheme.error)
			.use { implicit c =>
				ImageAndTextButton.contextual(Icons.delete.inButton, "Poista tukku") {
					DeleteShopWindow.display(shops, parentWindow).foreach { _.foreach { shopId =>
						shops = shops.filterNot { _.id == shopId } } }
				}
			}
		deleteShopButton.enabled = shops.nonEmpty
		
		Popup(menuButton, deleteShopButton, actorHandler, resizeAlignment = TopRight) { (cSize, wSize) =>
			Point(cSize.width - wSize.width, 0) }.display()
	}
}
