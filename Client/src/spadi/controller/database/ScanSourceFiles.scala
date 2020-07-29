package spadi.controller.database

import utopia.flow.util.FileExtensions._
import spadi.model.cached.{DatabaseStructureSource, VersionNumber}
import spadi.controller.Globals._
import spadi.model.enumeration.SqlFileType
import spadi.model.enumeration.SqlFileType.{Changes, Full}
import utopia.flow.util.IterateLines
import utopia.flow.util.StringExtensions._

/**
  * Used for searching for usable sql source files
  * @author Mikko Hilpinen
  * @since 28.7.2020, v1.2
  */
object ScanSourceFiles
{
	/**
	  * Finds the sql files to import
	  * @param currentDbVersion Current database version number. None if no database has been set up yet (default)
	  * @return Sql file sources to import. In order.
	  */
	def apply(currentDbVersion: Option[VersionNumber] = None) =
	{
		// Starts by finding all sql files in source directory
		sqlImportDirectory.allRegularFileChildrenOfType("sql").map { files =>
			// Categorizes the files by checking first comments
			val sources = files.flatMap { file =>
				val comments = IterateLines.fromPath(file) { _.filterNot { _.isEmpty }.takeWhile { _.startsWith("--") }
					.flatMap { line =>
						val (key, value) = line.drop(2).splitAtFirst(":")
						if (key.nonEmpty && value.nonEmpty)
							Some(key.trim.toLowerCase -> value.trim.toLowerCase)
						else
							None
					}.toMap
				}
				comments.toOption.flatMap { comments =>
					comments.get("version").map { versionString =>
						val fileType = comments.get("type").map(SqlFileType.forString).getOrElse(Full)
						DatabaseStructureSource(file, fileType, VersionNumber.parse(versionString),
							comments.get("from").orElse(comments.get("origin")).map(VersionNumber.parse))
					}
				}
			}.sortBy { _.targetVersion }
			
			if (sources.isEmpty)
				Vector()
			else
			{
				// Checks which of the sources need to be read
				currentDbVersion match
				{
					case Some(currentVersion) =>
						val latestVersion = sources.last.targetVersion
						// Reads only update files, if they form a path to the latest version.
						// Otherwise reads the full version, if present.
						val updates = sources.filter { _.fileType == Changes }.dropWhile { s =>
							s.targetVersion <= currentVersion || s.originVersion.exists { _ < currentVersion } }
						if (updates.headOption.exists { _.originVersion.forall { _ == currentVersion } } &&
							updates.lastOption.exists { _.targetVersion == latestVersion })
							updates
						else
							sources.findLast { _.fileType == Full }.map { Vector(_) }.getOrElse(updates)
					case None => sources.findLast { _.fileType == Full }.toVector
				}
			}
		}
	}
}
