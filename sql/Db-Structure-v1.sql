--
-- S-Price Database Structure
-- Type: Full
-- Version: v1.0
--

CREATE DATABASE s_price;
USE s_price;

-- Contains database updates
CREATE TABLE database_version
(
    id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    version VARCHAR(16) NOT NULL,
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX dv_version_idx (version),
    INDEX dv_creation_idx (created)

)Engine=InnoDB DEFAULT CHARSET=latin1;

-- Contains all shops
CREATE TABLE shop
(
    id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(32) NOT NULL,
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP

)Engine=InnoDB DEFAULT CHARSET=latin1;

-- Contains shop-specific sales groups
CREATE TABLE sale_group
(
    id INT NOT NULL,
    shop_id INT NOT NULL,
    group_identifier VARCHAR(12) NOT NULL,
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX sg_find_by_group_idx (shop_id, group_identifier),

    CONSTRAINT sg_s_shop_link_fk FOREIGN KEY sg_s_shop_link_idx (shop_id)
        REFERENCES shop(id) ON DELETE CASCADE

)Engine=InnoDB DEFAULT CHARSET=latin1;

-- Contains sales amount for each group (current & history)
CREATE TABLE sale_amount
(
    id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    group_id INT NOT NULL,
    price_modifier DOUBLE NOT NULL,
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deprecated_after DATETIME,

    INDEX sa_deprecation_idx (deprecated_after),

    CONSTRAINT sa_sg_group_link_fk FOREIGN KEY sa_sg_group_link_idx (group_id)
        REFERENCES sale_group(id) ON DELETE CASCADE

)Engine=InnoDB DEFAULT CHARSET=latin1;

-- One for each unique product. Shared between shops.
CREATE TABLE product
(
    id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    electric_id VARCHAR(12) NOT NULL,
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX p_electric_idx (electric_id)

)Engine=InnoDB DEFAULT CHARSET=latin1;

-- Links product with its name in each shop (current & history)
CREATE TABLE shop_product_name
(
    id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    product_id INT NOT NULL,
    shop_id INT NOT NULL,
    name_primary VARCHAR(64) NOT NULL,
    name_alternative VARCHAR(64),
    created TIMESTAMP NOT NULL,
    deprecated_after DATETIME,

    INDEX spn_find_by_name_idx (name_primary, name_alternative),
    INDEX spn_active_name_idx (deprecated_after),

    CONSTRAINT spn_p_product_link_fk FOREIGN KEY spn_p_product_link_idx (product_id)
        REFERENCES product(id) ON DELETE CASCADE,

    CONSTRAINT spn_s_shop_link_fk FOREIGN KEY spn_s_shop_link_idx (shop_id)
        REFERENCES shop(id) ON DELETE CASCADE

)Engine=InnoDB DEFAULT CHARSET=latin1;

-- Contains net prices for products for which this data is available
CREATE TABLE shop_product_net_price
(
    id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    product_id INT NOT NULL,
    shop_id INT NOT NULL,
    net_price DOUBLE NOT NULL,
    sale_unit VARCHAR(16) NOT NULL DEFAULT 'kpl',
    sale_count INT NOT NULL DEFAULT 1,
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deprecated_after DATETIME,

    INDEX spnp_active_price_idx (deprecated_after),

    CONSTRAINT spnp_p_product_link_fk FOREIGN KEY spnp_p_product_link_idx (product_id)
        REFERENCES product(id) ON DELETE CASCADE,

    CONSTRAINT spnp_s_shop_link_fk FOREIGN KEY spnp_s_shop_link_idx (shop_id)
        REFERENCES shop(id) ON DELETE CASCADE

)Engine=InnoDB DEFAULT CHARSET=latin1;

-- Contains base price information for products for which this data is available
-- NB: sale_group_id should be null only when this base price doesn't list a sale group identifier at all
CREATE TABLE shop_product_base_price
(
    id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    product_id INT NOT NULL,
    shop_id INT NOT NULL,
    sale_group_id INT,
    base_price DOUBLE NOT NULL,
    sale_unit VARCHAR(16) NOT NULL DEFAULT 'kpl',
    sale_count INT NOT NULL DEFAULT 1,
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deprecated_after DATETIME,

    INDEX spbp_active_price_idx (deprecated_after),

    CONSTRAINT spbp_p_product_link_fk FOREIGN KEY spbp_p_product_link_idx (product_id)
        REFERENCES product(id) ON DELETE CASCADE,

    CONSTRAINT spbp_s_shop_link_fk FOREIGN KEY spbp_s_shop_link_idx (shop_id)
            REFERENCES shop(id) ON DELETE CASCADE,

    CONSTRAINT spbp_sg_sale_link_fk FOREIGN KEY spbp_sg_sale_link_idx (sale_group_id)
        REFERENCES sale_group(id) ON DELETE CASCADE

)Engine=innoDB DEFAULT CHARSET=latin1;

-- Common instructions for reading both net and base price price documents
-- Type 1 refers to net price. Type 2 refers to base price.
CREATE TABLE price_key_map
(
    id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    shop_id INT NOT NULL,
    type_identifier INT NOT NULL,
    electric_id_key VARCHAR(32) NOT NULL,
    product_name_key VARCHAR(32) NOT NULL,
    product_name_key_alternative VARCHAR(32),
    price_key VARCHAR(32) NOT NULL,
    sale_unit_key VARCHAR(32),
    sale_count_key VARCHAR(32),
    sale_group_key VARCHAR(32),
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX pkm_timeline_idx (created),

    CONSTRAINT pkm_s_shop_link_fk FOREIGN KEY pkm_s_shop_link_idx (shop_id)
        REFERENCES shop(id) ON DELETE CASCADE

)Engine=InnoDB DEFAULT CHARSET=latin1;

-- Instructions for reading sale group documents
CREATE TABLE sale_group_key_map
(
    id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    shop_id INT NOT NULL,
    group_id_key VARCHAR(32) NOT NULL,
    sale_percent_key VARCHAR(32) NOT NULL,
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX sgkm_pkm_timeline_idx (created),

    CONSTRAINT sgkm_s_shop_link_fk FOREIGN KEY sgkm_s_shop_link_idx (shop_id)
        REFERENCES shop(id) ON DELETE CASCADE

)Engine=InnoDB DEFAULT CHARSET=latin1;