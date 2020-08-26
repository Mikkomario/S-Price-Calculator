package spadi.view.component

import spadi.controller.{Globals, Log}
import spadi.controller.database.access.multi.DbProducts
import spadi.model.stored.pricing.Shop
import spadi.view.dialog.{DeleteShopWindow, LoadingView}
import spadi.view.util.Icons
import utopia.reflection.component.swing.template.StackableAwtComponentWrapperWrapper
import spadi.view.util.Setup._
import utopia.flow.util.FileExtensions.RichPath
import utopia.genesis.image.Image
import utopia.genesis.shape.shape2D.Point
import utopia.reflection.component.swing.button.{ImageAndTextButton, ImageButton}
import utopia.reflection.component.swing.label.{ImageLabel, TextLabel}
import utopia.reflection.container.stack.StackLayout.Trailing
import utopia.reflection.container.swing.layout.multi.Stack
import utopia.reflection.container.swing.window.Popup
import utopia.reflection.shape.Alignment.TopRight
import utopia.reflection.shape.LengthExtensions._
import utopia.reflection.localization.LocalString._
import utopia.reflection.shape.StackLength

import scala.util.{Failure, Success}

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
	private val versionNumberLabel = context.forTextComponents().mapFont { _ * 0.8 }.use { implicit c =>
		TextLabel.contextual(s"- ${Globals.versionNumber}".noLanguageLocalizationSkipped)
	}
	// TODO: Add version number display
	private val view = context.forTextComponents().mapFont { _ * 1.2 }.mapInsets { _.withoutHorizontal }
		.use { implicit c =>
			val titlePart = Stack.rowWithItems(Vector(TextLabel.contextual("Suho"), versionNumberLabel),
				StackLength.fixedZero, layout = Trailing)
			val leftSide = Image.readFrom(resourceDirectory/"icons"/"logo-clear.png") match
			{
				case Success(logo) =>
					// Shrinks the logo down to accepted size
					val correctSizeLogo = logo.withLimitedHeight(titlePart.optimalHeight max menuButton.optimalHeight)
					Stack.buildRowWithContext(isRelated = true) { s =>
						s += ImageLabel.contextual(correctSizeLogo, alwaysFillsArea = false, isLowPriority = true)
						s += titlePart
					}
				case Failure(error) =>
					Log(error, "Couldn't read logo icon")
					titlePart
			}
			
			Stack.rowWithItems(Vector(leftSide, menuButton), margins.small.upscaling.expanding)
				.framed(margins.small.downscaling, color)
		}
	
	
	// IMPLEMENTED	-----------------------
	
	override protected def wrapped = view
	
	
	// OTHER	---------------------------
	
	private def displayMenu(): Unit =
	{
		val context = baseContext.inContextWithBackground(color).forTextComponents()
		
		val deleteShopButton = context.forCustomColorButtons(colorScheme.error).use { implicit c =>
			ImageAndTextButton.contextual(Icons.delete.inButton, "Poista tukku") {
				DeleteShopWindow.display(shops, parentWindow).foreach { _.foreach { shopId =>
					shops = shops.filterNot { _.id == shopId } } }
			}
		}
		deleteShopButton.enabled = shops.nonEmpty
		
		val cleanHistoryButton = context.forPrimaryColorButtons.use { implicit c =>
			ImageAndTextButton.contextual(Icons.clean.inButton, "Poista historiatiedot") {
				// Deletes deprecated product data. Displays a loading view while doing so
				// TODO: When history can actually be used for something, add an additional prompt here to make
				//  sure the user knows what they're doing
				val (progressPointer, future) = DbProducts.deleteDeprecatedDataAsync()
				val loadingView = new LoadingView(progressPointer)
				loadingView.display(parentWindow).foreach { _ =>
					future.foreach {
						case Success(_) => Fields.messageDialog("Valmista",
							"Historiatiedot on nyt poistettu onnistuneesti").display(parentWindow)
						case Failure(error) => Fields.errorDialog(
							"Historiatietojen poistaminen epäonnistui.\nVirheilmoitus:%s".autoLocalized
								.interpolated(Vector(error.getLocalizedMessage))).display(parentWindow)
					}
				}
			}
		}
		
		val popupContent = Stack.buildColumnWithContext(isRelated = true) { s =>
			s += deleteShopButton
			s += cleanHistoryButton
		}(context).framed(margins.small.any, color)
		
		Popup(menuButton, popupContent, actorHandler, resizeAlignment = TopRight) { (cSize, wSize) =>
			Point(cSize.width - wSize.width, 0) }.display()
	}
}
