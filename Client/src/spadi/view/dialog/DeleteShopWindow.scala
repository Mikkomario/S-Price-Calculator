package spadi.view.dialog

import spadi.controller.Log
import spadi.controller.database.access.multi.DbProducts
import spadi.controller.database.access.single.DbShop
import spadi.model.stored.pricing.Shop
import spadi.view.component.Fields
import utopia.reflection.component.swing.label.TextLabel
import spadi.view.util.Setup._
import utopia.reflection.container.swing.window.interaction.{InputRowBlueprint, RowGroups}
import utopia.reflection.localization.DisplayFunction

import scala.concurrent.Future
import scala.util.{Failure, Success}

object DeleteShopWindow
{
	private implicit val languageCode: String = "fi"
	
	/**
	  * Displays a shop deletion window and handles user actions
	  * @param shops Shops from which to select the deleted shop
	  * @param parentWindow Window that will host this window (optional)
	  * @return Id of the deleted shop. None if no shop was deleted. Asynchronous.
	  */
	def display(shops: Iterable[Shop], parentWindow: Option[java.awt.Window] = None) =
	{
		// Displays the dialog
		new DeleteShopWindow(shops).display(parentWindow).flatMap {
			case Some(shopId) =>
				Fields.deletionQuestionDialog(
					"Haluatko varmasti poistaa kaikki tätä tukkua koskevat tiedot.\nTätä toimintoa ei voi peruuttaa.")
					.display(parentWindow)
					.map { shouldDelete =>
						if (shouldDelete)
						{
							connectionPool.tryWith { implicit connection =>
								DbShop(shopId).delete()
								DbProducts.deleteProductsWithoutShopData()
							} match
							{
								case Success(_) => Fields.messageDialog("Tiedot poistettu",
									"Kaikki tukkua koskevat tiedot on nyt poistettu").display(parentWindow)
								case Failure(error) =>
									Log(error, s"Failed to delete shop with id $shopId")
									Fields.errorDialog(s"Tukun tietojen poistaminen epäonnistui.\nVirheilmoitus: ${
										error.getLocalizedMessage}").display(parentWindow)
							}
							// Once deletion is complete, returns the id of the deleted shop
							Some(shopId)
						}
						else
							None
					}
			case None => Future.successful(None)
		}
	}
}

/**
  * Used for deleting shop data from the DB
  * @author Mikko Hilpinen
  * @since 15.8.2020, v1.2
  */
class DeleteShopWindow(shops: Iterable[Shop]) extends InputWindow[Option[Int]]
{
	// ATTRIBUTES	------------------------------
	
	private implicit val languageCode: String = "fi"
	
	private val shopSelectionField = inputContext.forGrayFields.use { implicit c => Fields.dropDown[Shop](
		"Ei tukkuja valittavana", "Valitse",
		DisplayFunction.noLocalization[Shop] { _.name })
	}
	
	
	// INITIAL CODE	------------------------------
	
	shopSelectionField.content = shops.toVector.sortBy { _.name }
	
	
	// IMPLEMENTED	------------------------------
	
	override protected def header = Some(backgroundContext.forTextComponents().use { implicit c =>
		TextLabel.contextual("Valitse tukku jonka tiedot haluat poistaa", isHint = true)
	})
	
	override protected def fields = Vector(RowGroups.singleRow(
		new InputRowBlueprint("Poistettava tukku", shopSelectionField)))
	
	override protected def additionalButtons = Vector()
	
	override protected def produceResult = shopSelectionField.value match
	{
		case Some(shop) => Right(Some(shop.id))
		case None => Left(shopSelectionField -> "Tämä tieto tarvitaan")
	}
	
	override protected def defaultResult = None
	
	override protected def title = "Poista tukun tiedot"
}
