package spadi.model.cached.read

import utopia.flow.datastructure.immutable.{ModelDeclaration, PropertyDeclaration}
import utopia.flow.generic.{FromModelFactoryWithSchema, ModelConvertible, StringType}

/**
  * A common trait for key mapping implementations, which are used for excel reading
  * @author Mikko Hilpinen
  * @since 9.5.2020, v1
  */
trait KeyMapping[+A] extends FromModelFactoryWithSchema[A] with ModelConvertible
{
	// ABSTRACT -------------------------------
	
	/**
	  * @return Keys required by this mapping
	  */
	def requiredKeys: Set[String]
	
	
	// IMPLEMENTED  ---------------------------
	
	override def schema = ModelDeclaration(requiredKeys.map
	{ k => PropertyDeclaration(k, StringType) })
}
