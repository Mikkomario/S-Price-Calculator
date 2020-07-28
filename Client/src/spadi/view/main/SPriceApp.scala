package spadi.view.main

import spadi.controller.container.ShopData
import spadi.controller.read.ReadProducts
import spadi.controller.{Log, ScreenSizeOverrideSetup}
import spadi.model.cached.ProgressState
import spadi.model.cached.pricing.product.{ProductBasePrice, ProductPrice, SalesGroup}
import spadi.model.cached.pricing.shop.Shop
import spadi.view.component.Fields
import spadi.view.controller.{MainVC, NewFileConfigurationUI}
import spadi.view.dialog.LoadingView
import utopia.flow.async.AsyncExtensions._
import utopia.flow.datastructure.mutable.PointerWithEvents
import utopia.flow.util.CollectionExtensions._
import utopia.genesis.generic.GenesisDataType
import utopia.reflection.container.swing.window.Frame
import utopia.reflection.container.swing.window.WindowResizePolicy.Program
import utopia.reflection.shape.Alignment
import utopia.reflection.util.MultiFrameSetup
import utopia.reflection.localization.LocalString._

import scala.util.{Failure, Success, Try}

/**
 * The main app for project client
 * @author Mikko Hilpinen
 * @since 9.5.2020, v1
 */
object SPriceApp extends App
{
	System.setProperty("prism.allowhidpi", "false")
	
	GenesisDataType.setup()
	
	import spadi.view.util.Setup._
	
	private implicit val languageCode: String = "fi"
	
	private val setup = new MultiFrameSetup(actorHandler)
	setup.start()
	
	// Makes sure the screen size is read correctly
	ScreenSizeOverrideSetup.prepareBlocking()
	
	// Reads / updates product data. Displays a loading screen while doing so
	val (asyncReadResult, progressPointer) = ReadProducts.async()
	val products = new LoadingView(progressPointer).display().waitFor().flatMap { _ => asyncReadResult.waitForResult() } match
	{
		case Success(result) =>
			result match
			{
				case Right(readData) => handleReadResult(readData)
				case Left(filesWithoutMappings) =>
					// Configures settings and then reads data again
					NewFileConfigurationUI.configureBlocking(Left(filesWithoutMappings))
					val (secondReadFuture, progressPointer) = ReadProducts.asyncIgnoringUnmappedFiles()
					new LoadingView(progressPointer).display().waitFor().flatMap { _ => secondReadFuture.waitForResult() } match
					{
						case Success(readData) => handleReadResult(readData)
						case Failure(error) =>
							Log(error, "Failed to read products data after file configuring")
							Fields.errorDialog("Tuotetietojen lukeminen epäonnistui.\nVirheilmoitus: %s"
								.autoLocalized.interpolated(Vector(error.getLocalizedMessage))).display().waitFor()
							Vector()
					}
			}
		case Failure(error) =>
			Log(error, "Failed to read products data")
			Fields.errorDialog("Tuotetietojen lukeminen epäonnistui.\nVirheilmoitus: %s"
				.autoLocalized.interpolated(Vector(error.getLocalizedMessage))).display().waitFor()
			Vector()
	}
	
	// Displays a loading screen while setting up the VC
	val frameProgressPointer = new PointerWithEvents(ProgressState.initial("Käsitellään tuotetietoja"))
	val loadCompletion = new LoadingView(frameProgressPointer).display()
	val sortedProducts = products.sortBy { _.productId }
	frameProgressPointer.value = ProgressState(0.1, "Luodaan käyttöliittymäkomponentteja")
	val frame = Frame.windowed(new MainVC(sortedProducts), "S-Padi Hintalaskuri",
		resizePolicy = Program, resizeAlignment = Alignment.TopLeft)
	frame.setToCloseOnEsc()
	frame.setToExitOnClose()
	frameProgressPointer.value = ProgressState.finished("Käyttöliittymä valmis käyttöön")
	
	// When loading screen closes, opens the main view
	loadCompletion.onComplete { _ => setup.display(frame) }
	
	private def handleReadResult(
		readData: Option[Vector[(Shop, Try[Either[(Vector[ProductBasePrice], Vector[SalesGroup]), Vector[ProductPrice]]])]]) =
	{
		val progressPointer = new PointerWithEvents(ProgressState.initial("Käsitellään tuotteita"))
		val loadCompletion = new LoadingView(progressPointer).display()
		val products = readData match
		{
			case Some(newData) =>
				val (failures, successes) = newData.dividedWith { case (shop, readResult) =>
					readResult match
					{
						case Success(success) => Right(shop -> success)
						case Failure(e) => Left(shop -> e)
					}
				}
				if (failures.nonEmpty)
				{
					// Displays an error message in case some reads failed
					failures.foreach { case (shop, e) => Log(e, s"Failed to read data for ${shop.name}") }
					val failedShopNames = failures.map { _._1.name }.mkString(", ")
					val failureMessage =
					{
						if (failures.size > 1)
							"Seuraavien tukkujen tietoja ei voitu lukea: ${shops}.\nVirheilmoitus: ${error}"
								.autoLocalized.interpolated(
								Map("shops" -> failedShopNames, "error" -> failures.head._2.getLocalizedMessage))
						else
							"${shop} tietoja ei voitu lukea.\nVirheilmoitus: ${error}".autoLocalized.interpolated(
								Map("shop" -> failedShopNames, "error" -> failures.head._2.getLocalizedMessage))
					}
					// Blocks while the error message is being displayed
					Fields.errorDialog(failureMessage).display().waitFor()
				}
				ShopData.updateProducts(successes)
				progressPointer.value = ProgressState(0.9, "Tukkujen tiedot päivitetty")
				ShopData.products
			case None => ShopData.products
		}
		progressPointer.value = ProgressState.finished("Tuotteet valmiina")
		loadCompletion.waitFor()
		products
	}
}
