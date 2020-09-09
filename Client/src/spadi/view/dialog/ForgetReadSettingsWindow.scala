package spadi.view.dialog

import spadi.controller.Log
import spadi.controller.database.access.multi.{DbPriceKeyMappings, DbSaleKeyMappings}
import spadi.model.cached.ProgressState
import spadi.model.stored.pricing.Shop
import spadi.view.component.Fields
import spadi.view.controller.ShopSelectionVC
import spadi.view.util.Setup._
import utopia.flow.datastructure.mutable.PointerWithEvents
import utopia.genesis.util.Screen
import utopia.reflection.component.swing.display.MultiLineTextView
import utopia.reflection.container.swing.window.interaction.{InputRowBlueprint, RowGroups}
import utopia.reflection.localization.LocalString._

import scala.util.{Failure, Success}

object ForgetReadSettingsWindow
{
	private implicit val languageCode: String = "fi"
	
	/**
	  * Displays a dialog for selecting shop from which to delete read settings. Handles user input and data deletion,
	  * as well as informing the user about operation results.
	  * @param shops Available shops
	  * @param parentWindow Window that will host this window (optional)
	  */
	def display(shops: Vector[Shop], parentWindow: Option[java.awt.Window] = None) =
		new ForgetReadSettingsWindow(shops).display(parentWindow).foreach { _.foreach { shop =>
			val progressPointer = new PointerWithEvents(ProgressState.initial("Poistetaan tuotelukuasetuksia"))
			val loadingCompletion = new LoadingView(progressPointer).display(parentWindow)
			connectionPool.tryWith { implicit connection =>
				DbPriceKeyMappings.forShopWithId(shop.id).delete()
				progressPointer.value = ProgressState(0.5, "Poistetaan alennuslukuasetuksia")
				DbSaleKeyMappings.forShopWithId(shop.id).delete()
			} match
			{
				case Success(_) =>
					progressPointer.value = ProgressState.finished("Lukuasetukset poistettu")
					loadingCompletion.onComplete { _ => Fields.successDialog(
						s"Tukun %s lukuasetukset on nyt poistettu.\nVoit määrittää asetukset uudestaan kun seuraavan kerran luet tukun tietoja."
							.autoLocalized.interpolated(Vector(shop.name))).display(parentWindow) }
				case Failure(error) =>
					progressPointer.value = ProgressState.finished("Lukuasetusten poistaminen epäonnistui")
					Log(error, s"Failed to delete read settings for shop ${shop.id} (${shop.name})")
					loadingCompletion.onComplete { _ => Fields.errorDialog(
						"Tietojen poistaminen epäonnistui.\nVirheilmoitus: %s".autoLocalized
							.interpolated(Vector(error.getLocalizedMessage))).display(parentWindow) }
			}
		} }
}

/**
  * A dialog used for deleting shop data read settings
  * @author Mikko Hilpinen
  * @since 9.9.2020, v1.2.3
  */
class ForgetReadSettingsWindow(shops: Vector[Shop]) extends InputWindow[Option[Shop]]
{
	import ForgetReadSettingsWindow._
	
	// ATTRIBUTES	---------------------------
	
	private lazy val shopField = inputContext.forGrayFields.use { implicit c => ShopSelectionVC(shops) }
	
	
	// IMPLEMENTED	---------------------------
	
	override protected def header = Some(backgroundContext.forTextComponents().use { implicit c =>
		MultiLineTextView.contextual("Valitse tukku jonka lukuasetukset haluat määrittää uudelleen",
			Screen.width / 3, useLowPriorityForScalingSides = true, isHint = true)
	})
	
	override protected def fields = inputContext.forGrayFields.use { implicit c =>
		Vector(RowGroups.singleRow(new InputRowBlueprint("Tukku", shopField)))
	}
	
	override protected def additionalButtons = Vector()
	
	override protected def produceResult = shopField.value match
	{
		case Some(shop) => Right(Some(shop))
		case None => Left(shopField -> "Tämä tieto tarvitaan")
	}
	
	override protected def defaultResult = None
	
	override protected def title = "Lukuasetusten unohtaminen"
}
