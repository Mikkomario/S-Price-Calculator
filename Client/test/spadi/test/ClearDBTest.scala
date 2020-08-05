package spadi.test

import spadi.controller.database.DbSetup
import utopia.genesis.generic.GenesisDataType

/**
  * Clears all database data
  * @author Mikko Hilpinen
  * @since 5.8.2020, v1.2
  */
object ClearDBTest extends App
{
	GenesisDataType.setup()
	DbSetup.clear()
	println("Done")
	
	System.exit(0)
}
