package spadi.test

import spadi.controller.Log
import utopia.genesis.generic.GenesisDataType

/**
  * Tests error logging
  * @author Mikko Hilpinen
  * @since 12.8.2020, v1.2
  */
object LogTest extends App
{
	GenesisDataType.setup()
	Log(new NullPointerException("Test exception"), "Error message")
	
	println("Done")
}
