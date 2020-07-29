package spadi.model.cached

import java.nio.file.Path

import spadi.model.enumeration.SqlFileType
import spadi.model.enumeration.SqlFileType.{Changes, Full}

/**
  * Represents a file from which database structure can be imported. Contains important metadata.
  * @author Mikko Hilpinen
  * @since 28.7.2020, v1.2
  */
case class DatabaseStructureSource(path: Path, fileType: SqlFileType, targetVersion: VersionNumber,
								   originVersion: Option[VersionNumber] = None)
{
	override def toString = fileType match
	{
		case Full => s"$path (Full $targetVersion)"
		case Changes => s"$path (Changes${originVersion.map { v => s" from $v" }} to $targetVersion)"
	}
}
