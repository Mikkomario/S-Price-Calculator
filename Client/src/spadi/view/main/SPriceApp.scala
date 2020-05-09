package spadi.view.main

import spadi.view.controller.MainVC
import utopia.genesis.generic.GenesisDataType
import utopia.reflection.container.swing.window.Frame
import utopia.reflection.container.swing.window.WindowResizePolicy.Program
import utopia.reflection.shape.Alignment
import utopia.reflection.util.SingleFrameSetup

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
	val frame = Frame.windowed(new MainVC, "S-Padi Hintalaskuri", resizePolicy = Program,
		resizeAlignment = Alignment.TopLeft)
	
	new SingleFrameSetup(actorHandler, frame).start()
}
