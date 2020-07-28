--
-- S-Price Database Structure
-- Type: Full
-- Version: v1.0
--

-- Contains database updates
CREATE TABLE database_version
(
    id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    version VARCHAR(16) NOT NULL,
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX dv_version_idx (version),
    INDEX dv_creation_idx (created)

)Engine=InnoDB DEFAULT CHARSET=latin1;

-- Inserts initial version number
INSERT INTO database_version (version) VALUES ("v1.0");