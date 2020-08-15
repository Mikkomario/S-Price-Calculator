package spadi.view.main

import spadi.controller.database.DbSetup
import spadi.controller.database.access.multi.DbShops
import spadi.controller.{Log, ScreenSizeOverrideSetup}
import spadi.model.cached.ProgressState
import spadi.model.stored.pricing.Product
import spadi.view.component.Fields
import spadi.view.controller.{MainVC, NewFileConfigurationUI}
import spadi.view.dialog.LoadingView
import utopia.flow.async.AsyncExtensions._
import utopia.flow.datastructure.mutable.PointerWithEvents
import utopia.reflection.container.swing.window.Frame
import utopia.reflection.container.swing.window.WindowResizePolicy.Program
import utopia.reflection.localization.LocalString._
import utopia.reflection.shape.Alignment
import utopia.reflection.util.MultiFrameSetup

import scala.util.{Failure, Success}

/**
 * The main app for project client
 * @author Mikko Hilpinen
 * @since 9.5.2020, v1
 */
object SPriceApp extends App
{
	System.setProperty("prism.allowhidpi", "false")
	
	import spadi.view.util.Setup._
	private implicit val languageCode: String = "fi"
	
	val setup = new MultiFrameSetup(actorHandler)
	setup.start()
	
	// Sets up local database access (displays a loading view during database setup)
	private val dbSetupProgressPointer = new PointerWithEvents(ProgressState.initial("Pystytetään tietokantaa"))
	val dbLoadingCompletion = new LoadingView(dbSetupProgressPointer).display()
	DbSetup.setup(dbSetupProgressPointer) match
	{
		case Success(dbVersion) =>
			println(s"Using database version ${dbVersion.number}")
			dbLoadingCompletion.waitFor()
			
			// Makes sure the screen size is read correctly
			ScreenSizeOverrideSetup.prepareBlocking()
			
			// Performs product data updates
			NewFileConfigurationUI.readAndConfigureBlocking()
			
			// Reads shop data and initial products from DB
			connectionPool.tryWith { implicit connection =>
				val shops = DbShops.all
				val defaultProducts = Vector[Product]() // TODO: List used products here
				shops -> defaultProducts
			} match
			{
				case Success((shops, defaultProducts)) =>
					// Displays a loading screen while setting up the VC
					val frameProgressPointer = new PointerWithEvents(ProgressState.initial("Käsitellään tuotetietoja"))
					val loadCompletion = new LoadingView(frameProgressPointer).display()
					frameProgressPointer.value = ProgressState(0.1, "Luodaan käyttöliittymäkomponentteja")
					val frame = Frame.windowed(new MainVC(shops, defaultProducts), "S-Padi Hintalaskuri",
						resizePolicy = Program, resizeAlignment = Alignment.TopLeft)
					frame.setToCloseOnEsc()
					frame.setToExitOnClose()
					frameProgressPointer.value = ProgressState.finished("Käyttöliittymä valmis käyttöön")
					
					// When loading screen closes, opens the main view
					loadCompletion.onComplete { _ => setup.display(frame) }
				case Failure(error) =>
					Log(error, "Failed to read shop & product data")
					Fields.errorDialog("Tuotetietojen lukeminen epäonnistui.\nVirheilmoitus: %s"
						.autoLocalized.interpolated(Vector(error.getLocalizedMessage))).displayBlocking()
					System.exit(1)
			}
		case Failure(error) =>
			Log(error, "Failed to set up the database")
			Fields.errorDialog("Tietokannan alustus epäonnistui.\nVirheilmoitus: %s"
				.autoLocalized.interpolated(Vector(error.getLocalizedMessage))).displayBlocking()
			System.exit(1)
	}
}
