package spadi.view.main

import spadi.controller.{ReadProducts, ShopData}
import spadi.view.controller.MainVC
import utopia.flow.util.CollectionExtensions._
import utopia.genesis.generic.GenesisDataType
import utopia.reflection.container.swing.window.Frame
import utopia.reflection.container.swing.window.WindowResizePolicy.Program
import utopia.reflection.shape.Alignment
import utopia.reflection.util.SingleFrameSetup

import scala.util.{Failure, Success}

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
	
	// Reads / updates product data
	val products = ReadProducts() match
	{
		case Success(result) =>
			result match
			{
				case Right(readData) =>
					readData match
					{
						case Some(newData) =>
							val (failures, successes) = newData.divideBy { _._2.isSuccess }
							if (failures.nonEmpty)
							{
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
				case Left(filesWithoutMappings) =>
					// TODO: Add UI for handling these
					println(s"Missing mappings for ${filesWithoutMappings.size} files:")
					filesWithoutMappings.foreach(println)
					Vector()
			}
		case Failure(error) =>
			println("Failed to read products data")
			error.printStackTrace()
			Vector()
	}
	
	val frame = Frame.windowed(new MainVC(products.sortBy { _.productId }), "S-Padi Hintalaskuri",
		resizePolicy = Program, resizeAlignment = Alignment.TopLeft)
	
	new SingleFrameSetup(actorHandler, frame).start()
}
