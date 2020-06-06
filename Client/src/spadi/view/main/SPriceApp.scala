package spadi.view.main

import spadi.controller.{ReadProducts, ShopData}
import spadi.model.{ProductBasePrice, ProductPrice, SalesGroup, Shop}
import spadi.view.controller.{MainVC, NewFileConfigurationUI}
import utopia.flow.util.CollectionExtensions._
import utopia.genesis.generic.GenesisDataType
import utopia.reflection.container.swing.window.Frame
import utopia.reflection.container.swing.window.WindowResizePolicy.Program
import utopia.reflection.shape.Alignment
import utopia.reflection.util.MultiFrameSetup

import scala.util.{Failure, Success, Try}

/**
 * The main app for project client
 * @author Mikko Hilpinen
 * @since 9.5.2020, v1
 */
object SPriceApp extends App
{
	GenesisDataType.setup()
	
	import spadi.view.util.Setup._
	
	private implicit val languageCode: String = "fi"
	
	private val setup = new MultiFrameSetup(actorHandler)
	setup.start()
	
	// Reads / updates product data
	val products = ReadProducts() match
	{
		case Success(result) =>
			result match
			{
				case Right(readData) => handleReadResult(readData)
				case Left(filesWithoutMappings) =>
					// Configures settings and then reads data again
					NewFileConfigurationUI.configureBlocking(Left(filesWithoutMappings))
					ReadProducts.ignoringUnmappedFiles() match
					{
						case Success(readData) => handleReadResult(readData)
						case Failure(error) =>
							println("Failed to read products data")
							error.printStackTrace()
							Vector()
					}
			}
		case Failure(error) =>
			println("Failed to read products data")
			error.printStackTrace()
			Vector()
	}
	
	val frame = Frame.windowed(new MainVC(products.sortBy { _.productId }), "S-Padi Hintalaskuri",
		resizePolicy = Program, resizeAlignment = Alignment.TopLeft)
	frame.setToCloseOnEsc()
	frame.setToExitOnClose()
	setup.display(frame)
	
	private def handleReadResult(
		readData: Option[Vector[(Shop, Try[Either[(Vector[ProductBasePrice], Vector[SalesGroup]), Vector[ProductPrice]]])]]) =
	{
		readData match
		{
			case Some(newData) =>
				val (failures, successes) = newData.divideBy { _._2.isSuccess }
				if (failures.nonEmpty)
				{
					// TODO: Add separate failure handling UI
					println(s"Failed to read ${failures.size}/${newData.size} shop's data")
					failures.foreach { case (shop, failure) =>
						println(s"${shop.name} (${shop.id}):")
						failure.failure.foreach { _.printStackTrace() }
					}
				}
				ShopData.updateProducts(successes.map { case (shop, result) => shop -> result.get })
				ShopData.products
			case None => ShopData.products
		}
	}
}
