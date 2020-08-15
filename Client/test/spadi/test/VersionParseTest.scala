package spadi.test

import spadi.model.cached.VersionNumber
import utopia.genesis.generic.GenesisDataType

/**
  * Tests version number parsing
  * @author Mikko Hilpinen
  * @since 29.7.2020, v1.2
  */
object VersionParseTest extends App
{
	GenesisDataType.setup()
	
	assert(VersionNumber.parse("v1.2.3.0") == VersionNumber(1, 2, 3, 0))
	assert(VersionNumber(1, 2, 3, 0).toString.startsWith("v1.2.3"))
	
	assert(VersionNumber.parse("v1.2-beta") == VersionNumber(1, 2).withSuffix("beta"))
	assert(VersionNumber(1, 2).withSuffix("beta").toString == "v1.2-beta")
	
	println("Done")
}
