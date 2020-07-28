package spadi.controller.database

import utopia.flow.util.FileExtensions._
import spadi.model.cached.VersionNumber
import spadi.controller.Globals._

/**
  * Used for searching for usable sql source files
  * @author Mikko Hilpinen
  * @since 28.7.2020, v1.2
  */
object ScanSourceFiles
{
	def apply(currentDbVersion: Option[VersionNumber]) =
	{
		// Starts by finding all sql files in source directory
		sqlImportDirectory.allRegularFileChildrenOfType("sql").map { files =>
			// Categorizes the files by checking first comments
			files.flatMap { file =>
				
				???
			}
		}
	}
}
