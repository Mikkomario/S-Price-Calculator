package spadi.model.cached

import utopia.reflection.localization.LocalizedString

object ProgressState
{
	/**
	 * @param description Progress start description
	 * @return A new progress state with 0% progress
	 */
	def initial(description: LocalizedString) = ProgressState(0, description)
	
	/**
	 * @param description Description of progress completion
	 * @return A new progress state with 100% progress
	 */
	def finished(description: LocalizedString) = ProgressState(1, description)
}

/**
 * Represents a state of progress between 0 and 100%. Contains a progress description as well.
 * @author Mikko Hilpinen
 * @since 8.7.2020, v1.1.2
 * @param progress Process progress, between 0 and 1
 * @param description Displayable progress description
 */
case class ProgressState(progress: Double, description: LocalizedString)
