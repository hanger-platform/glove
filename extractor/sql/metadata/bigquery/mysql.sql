SELECT * FROM (

	SELECT * FROM (
		SELECT DISTINCT
			0 AS ordinal_position,
			CASE WHEN '${CUSTOM_PRIMARY_KEY}'!= '' THEN CONCAT('CONCAT(','${CUSTOM_PRIMARY_KEY}',')',' AS custom_primary_key') ELSE CONCAT('CONCAT(', GROUP_CONCAT(LOWER(column_name)),')',' AS custom_primary_key')  end AS fields,
			CASE WHEN '${CUSTOM_PRIMARY_KEY}'!= '' THEN CONCAT('CONCAT(','${CUSTOM_PRIMARY_KEY}',')') ELSE CONCAT('CONCAT(', GROUP_CONCAT(LOWER(column_name)),')') END AS casting,
			'' 													AS field_type,
			'{"name": "custom_primary_key","type":"STRING"}' 	AS json,
			'custom_primary_key' 								AS column_name,
			1 													AS column_key,
			''                                                  AS encoding
		FROM
			information_schema.columns c
		WHERE
			LOWER( c.table_schema ) = LOWER('${INPUT_TABLE_SCHEMA}')
			AND
			LOWER( c.table_name ) = LOWER('${INPUT_TABLE_NAME}')
			AND
			c.column_key="PRI"
		ORDER BY 
			c.ordinal_position
	) x

	UNION ALL

    SELECT DISTINCT
        ordinal_position,
        CASE
            WHEN data_type IN ('datetime','timestamp') THEN CONCAT('COALESCE(DATE_FORMAT(IF( WEEKDAY(',column_name,') IS NULL', ',' , '''1900-01-01 00:00:00''', ',', column_name, '),', '''%Y-%m-%d %T', '${TIMEZONE_OFFSET}', '''), ', '''1900-01-01 00:00:00''', ') AS `',column_name,'`')
            WHEN data_type = 'date' THEN CONCAT('COALESCE(DATE_FORMAT(IF( WEEKDAY(',column_name,') IS NULL', ',' , '''1900-01-01''', ',', column_name, '),', '''%Y-%m-%d', '''), ', '''1900-01-01''', ') AS `',column_name,'`')
			WHEN data_type IN ('tinyint','smallint','mediumint', 'int', 'bigint') THEN CONCAT('CAST(`',column_name,'` AS SIGNED) AS `',column_name,'`')
            WHEN data_type IN ('text', 'longtext', 'mediumtext', 'tinytext', 'varchar') then concat('`', column_name, '` AS `',column_name,'`' )
            WHEN data_type IN ('blob','mediumblob','longblob') THEN CONCAT('CONVERT(`', column_name, '` USING utf8) AS `',REPLACE(column_name,' ','_'),'`' )
 			WHEN data_type = 'time'  THEN CONCAT('CAST(`', column_name, '` AS CHAR) AS `',REPLACE(column_name,' ','_'),'`' )
            ELSE CONCAT('`',column_name,'`')
        END AS fields,
        CASE
            WHEN data_type IN ('datetime','timestamp') THEN CONCAT('COALESCE(DATE_FORMAT(IF( WEEKDAY(',column_name,') IS NULL', ',' , '''1900-01-01 00:00:00''', ',', column_name, '),', '''%Y-%m-%d %T', '${TIMEZONE_OFFSET}', '''), ', '''1900-01-01 00:00:00''', ')')
            WHEN data_type = 'date' THEN CONCAT('COALESCE(DATE_FORMAT(IF( WEEKDAY(',column_name,') IS NULL', ',' , '''1900-01-01''', ',', column_name, '),', '''%Y-%m-%d', '''), ', '''1900-01-01''', ')' )
			WHEN data_type IN ('tinyint','smallint','mediumint', 'int', 'bigint') THEN CONCAT('CAST(`',column_name,'` AS SIGNED)')
            WHEN data_type IN ('text', 'longtext', 'mediumtext', 'tinytext', 'varchar') then concat('`', column_name, '`' )
            WHEN data_type IN ('blob','mediumblob','longblob') THEN CONCAT('CONVERT(`',column_name,'` USING utf8)')
 			WHEN data_type = 'time' THEN CONCAT('CAST(`',column_name,'` AS CHAR)')            
			ELSE CONCAT('`',column_name,'`')
        END AS casting,
		'' AS field_type,
		CONCAT('{"name": "', LOWER( column_name ), '","type":', 
			IF( data_type IN ("tinyint","smallint","mediumint", "int", "bit", "bigint"),'"INTEGER"', 
			IF( data_type IN ("float","double", "decimal"),'"FLOAT"', 
			IF( data_type IN ("timestamp","datetime"),'"TIMESTAMP"',
			IF( data_type = "date",'"DATE"', 
			IF( data_type = "boolean",'"BOOLEAN"', 
			IF( data_type = "time",'"TIME"','"STRING"' )))))), ' }'
		) AS json,
		LOWER( column_name ) AS column_name,
        0 AS column_key,
		'' AS encoding
    FROM
        information_schema.columns c
    WHERE 1=1
	AND LOWER( c.table_schema ) = LOWER('${INPUT_TABLE_SCHEMA}')
   	AND LOWER( c.table_name ) = LOWER('${INPUT_TABLE_NAME}')
	AND UPPER(c.column_name) NOT IN (${METADATA_BLACKLIST})

    UNION ALL

    SELECT
        999 AS ordinal_position,
        CONCAT('CONCAT(','DATE_FORMAT(now(),','''','%Y-%m-%d %T', '''','),', '''' ,'${TIMEZONE_OFFSET}', '''',') AS etl_load_date') AS fields,
		CONCAT('CONCAT(','DATE_FORMAT(now(),','''','%Y-%m-%d %T', '''','),', '''' ,'${TIMEZONE_OFFSET}', '''',')') AS casting,
        '' 												AS field_type,
  		'{"name": "etl_load_date","type":"TIMESTAMP"}' 	AS json,
        'etl_load_date' 								AS column_name,
        0 												AS column_key,
		''                                              AS encoding
) x
ORDER BY x.ordinal_position
