SELECT * FROM (

	 SELECT DISTINCT
	    -1 AS ordinal_position,
	    CASE
	        WHEN '${PARTITION_TYPE}' = 'date' OR '${PARTITION_TYPE}' = 'timestamp' THEN 
	        	CONCAT('COALESCE(FORMAT(',column_name,',',
					CASE '${PARTITION_FORMAT}' 
						WHEN 'YYYY' 	THEN '''yyyy''' 
						WHEN 'YYYYMM' 	THEN '''yyyyMM''' 
						WHEN 'YYYYWW' 	THEN '''yyyyWW''' 
						WHEN 'YYYYMMDD' THEN '''yyyyMMdd''' 
					END,'),',
					CASE '${PARTITION_FORMAT}' 
						WHEN 'YYYY' 	THEN '''1900''' 
						WHEN 'YYYYMM' 	THEN '''190001''' 
						WHEN 'YYYYWW' 	THEN '''190001''' 
						WHEN 'YYYYMMDD' THEN '''19000101'''
						ELSE '''190001''' 
				END,') AS partition_field')
	        WHEN '${PARTITION_TYPE}' = 'id' THEN CONCAT('(( floor( COALESCE(CAST(',column_name,' AS TINYINT ),1) / ( ${PARTITION_LENGTH} + 0.01 ) ) + 1 ) * ${PARTITION_LENGTH}) AS partition_field')
		END AS fields,
	    CASE
	        WHEN '${PARTITION_TYPE}' = 'date' OR '${PARTITION_TYPE}' = 'timestamp' THEN 
	        	CONCAT('COALESCE(FORMAT(',column_name,',',
					CASE '${PARTITION_FORMAT}' 
						WHEN 'YYYY' 	THEN '''yyyy''' 
						WHEN 'YYYYMM' 	THEN '''yyyyMM''' 
						WHEN 'YYYYWW' 	THEN '''yyyyWW''' 
						WHEN 'YYYYMMDD' THEN '''yyyyMMdd''' 
					END,'),',
					CASE '${PARTITION_FORMAT}' 
						WHEN 'YYYY' 	THEN '''1900''' 
						WHEN 'YYYYMM' 	THEN '''190001''' 
						WHEN 'YYYYWW' 	THEN '''190001''' 
						WHEN 'YYYYMMDD' THEN '''19000101'''
						ELSE '''190001''' 
				END,')')
	        WHEN '${PARTITION_TYPE}' = 'id' THEN CONCAT('(( floor( COALESCE(CAST(',column_name,' AS TINYINT ),1) / ( ${PARTITION_LENGTH} + 0.01 ) ) + 1 ) * ${PARTITION_LENGTH})')
	    END AS casting,
	    'int' AS field_type,
		'{"name": "partition_field","type":["null", "int"], "default": null}' AS json,
	    'partition_field' 							 	AS column_name,
	    0 											 	AS column_key,
		'' 												AS encoding
	FROM
	    information_schema.columns c
	WHERE
		LOWER( c.table_schema ) = LOWER('${INPUT_TABLE_SCHEMA}')
		AND
		LOWER( c.table_name ) = LOWER('${INPUT_TABLE_NAME}')
		AND
		LOWER( c.column_name ) = LOWER('${PARTITION_FIELD}')

    UNION ALL

	SELECT DISTINCT 
		0 AS ordinal_position,
		CASE WHEN '${CUSTOM_PRIMARY_KEY}'!= '' THEN CONCAT('CONCAT(','${CUSTOM_PRIMARY_KEY}',')',' AS custom_primary_key') ELSE CONCAT('CONCAT(', STRING_AGG(LOWER(column_name),','),')',' AS custom_primary_key') END AS fields,
		CASE WHEN '${CUSTOM_PRIMARY_KEY}'!= '' THEN CONCAT('CONCAT(','${CUSTOM_PRIMARY_KEY}',')') ELSE CONCAT('CONCAT(', STRING_AGG(LOWER(column_name),','),')') END AS casting,
		'varchar(255)' AS field_type,
		'{"name": "custom_primary_key","type":["null", "string"], "default": null}' AS json,
		'custom_primary_key' AS column_name,
		1 AS column_key,
		'' AS encoding
	FROM
		INFORMATION_SCHEMA.TABLE_CONSTRAINTS AS TC
	INNER JOIN INFORMATION_SCHEMA.KEY_COLUMN_USAGE AS KU ON
		TC.CONSTRAINT_TYPE = 'PRIMARY KEY'
		AND 
		TC.CONSTRAINT_NAME = KU.CONSTRAINT_NAME
		AND 
		LOWER(KU.TABLE_SCHEMA) = LOWER('${INPUT_TABLE_SCHEMA}')
		AND 
		LOWER(KU.TABLE_NAME) = LOWER('${INPUT_TABLE_NAME}')
	ORDER BY 1;

	UNION ALL


    SELECT DISTINCT
        ordinal_position,
        CASE
            WHEN data_type IN ('datetime','timestamp') THEN CONCAT('COALESCE(DATE_FORMAT(IF( WEEKDAY(',column_name,') IS NULL', ',' , '''1900-01-01 00:00:00''', ',', column_name, '),', '''%Y-%m-%d %T', ' ${TIMEZONE_OFFSET}', '''), ', '''1900-01-01 00:00:00''', ') AS `',column_name,'`')
            WHEN data_type = 'date' THEN CONCAT('COALESCE(DATE_FORMAT(IF( WEEKDAY(',column_name,') IS NULL', ',' , '''1900-01-01''', ',', column_name, '),', '''%Y-%m-%d', '''), ', '''1900-01-01''', ') AS `',REPLACE(column_name,' ','_'),'`')
			WHEN data_type IN ('tinyint','smallint','mediumint', 'int', 'bigint') THEN CONCAT('CAST(`',column_name,'` AS SIGNED) AS `',REPLACE(column_name,' ','_'),'`')
            WHEN data_type IN ('text', 'longtext', 'mediumtext', 'tinytext', 'varchar') then concat('`', column_name, '` AS `',REPLACE(column_name,' ','_'),'`' )
            ELSE CONCAT('`',column_name,'`')
        END AS fields,
        CASE
            WHEN data_type IN ('datetime','timestamp') THEN CONCAT('COALESCE(DATE_FORMAT(IF( WEEKDAY(',column_name,') IS NULL', ',' , '''1900-01-01 00:00:00''', ',', column_name, '),', '''%Y-%m-%d %T', ' ${TIMEZONE_OFFSET}', '''), ', '''1900-01-01 00:00:00''', ')')
            WHEN data_type = 'date' THEN CONCAT('COALESCE(DATE_FORMAT(IF( WEEKDAY(',column_name,') IS NULL', ',' , '''1900-01-01''', ',', column_name, '),', '''%Y-%m-%d', '''), ', '''1900-01-01''', ')' )
			WHEN data_type IN ('tinyint','smallint','mediumint', 'int', 'bigint') THEN CONCAT('CAST(`',column_name,'` AS SIGNED)')
            WHEN data_type IN ('text', 'longtext', 'mediumtext', 'tinytext', 'varchar') then concat('`', column_name, '`' )
            ELSE CONCAT('`',column_name,'`')
        END AS casting,
		CASE data_type
			WHEN 'bit'          THEN 'int'
			WHEN 'tinyint'      THEN 'int'
			WHEN 'smallint'     THEN 'int'
			WHEN 'mediumint'    THEN 'int'
          	WHEN 'int'          THEN IF(column_type LIKE '%unsigned%', 'bigint', 'int') 
            WHEN 'bigint'       THEN 'bigint'
           	WHEN 'tinytext'     THEN 'varchar(65535)'
           	WHEN 'mediumtext'   THEN 'varchar(65535)'
           	WHEN 'text'         THEN 'varchar(65535)'
           	WHEN 'longtext'     THEN 'varchar(65535)'
            WHEN 'blob'         THEN 'varchar(65535)'
            WHEN 'mediumblob'   THEN 'varchar(65535)'
	        WHEN 'longblob'   	THEN 'varchar(65535)'	
            WHEN 'date'         THEN 'varchar(10)'
            WHEN 'datetime'     THEN 'varchar(19)'
            WHEN 'time'         THEN 'varchar(17)'
            WHEN 'timestamp'    THEN 'varchar(19)'
			WHEN 'decimal'      THEN CONCAT('decimal','(', IF( NUMERIC_PRECISION > 38, 38, NUMERIC_PRECISION ) ,',',NUMERIC_SCALE,')')
            WHEN 'double'       THEN CASE '${IS_SPECTRUM}' WHEN '1' THEN CASE '${HAS_ATHENA}' WHEN '1' THEN 'double' ELSE 'double precision' END ELSE 'double precision' END
            WHEN 'float'        THEN CASE '${IS_SPECTRUM}' WHEN '1' THEN CASE '${HAS_ATHENA}' WHEN '1' THEN 'double' ELSE 'double precision' END ELSE 'double precision' END
            WHEN 'set'          THEN 'varchar(255)'
            WHEN 'enum'         THEN 'varchar(255)'
            WHEN 'char'         THEN CONCAT('varchar','(', CHARACTER_MAXIMUM_LENGTH + ROUND( ( CHARACTER_MAXIMUM_LENGTH - 1 ) / 2 ),')')
            WHEN 'varchar'      THEN CONCAT('varchar','(', CHARACTER_MAXIMUM_LENGTH + ROUND( ( CHARACTER_MAXIMUM_LENGTH - 1 ) / 2 ),')')
			WHEN 'boolean' 		THEN 'boolean'
			ELSE 'varchar(255)'
        END AS field_type,
		CONCAT('{"name": "', LOWER( REPLACE(column_name,' ','_') ), '","type":', 
			IF( data_type IN ("tinyint","smallint","mediumint", "bit"), '["null", "int"]', 
			IF( data_type IN ("int") AND column_type LIKE '%unsigned%', '["null", "long"]', 
			IF( data_type IN ("int") AND column_type NOT LIKE '%unsigned%', '["null", "int"]', 
			IF( data_type IN ("bigint"), '["null", "long"]', 
			IF( data_type IN ("float","double"), '["null", "double"]', 
			IF( data_type IN ("decimal"), CONCAT( '["null", {"type":"fixed", "name": "', LOWER( REPLACE(column_name,' ','_') ) , '", "size":' , CAST( ROUND( IF( NUMERIC_PRECISION > 38, 38, NUMERIC_PRECISION ) ) / 2 AS SIGNED ) , ', "logicalType": "decimal", "precision":' , ROUND( IF( NUMERIC_PRECISION > 38, 38, NUMERIC_PRECISION ) ) , ', "scale":' , NUMERIC_SCALE , '}]' ), 
			IF( data_type = "timestamp",'["null", "string"]', 
			IF( data_type = "datetime",'["null", "string"]', 
			IF( data_type = "boolean",'["null", "boolean"]', 
			IF( data_type = "date",'["null", "string"]', 
			IF( data_type = "time",'["null", "string"]','["null", "string"]' ))))))))))
			), ' , "default": null}'
		) AS json,
		LOWER( REPLACE(column_name,' ','_') ) AS column_name,
        0 AS column_key,
		'' AS encoding
    FROM
        information_schema.columns c
    WHERE
		LOWER( c.table_schema ) = LOWER('${INPUT_TABLE_SCHEMA}')
   		AND
		LOWER( c.table_name ) = LOWER('${INPUT_TABLE_NAME}')
		AND 
		UPPER(COLUMN_NAME) NOT IN (${METADATA_BLACKLIST})

    UNION ALL

	SELECT
        999 AS ordinal_position,
        CONCAT('CONCAT(','FORMAT(GETDATE(),','''','yyyy-MM-dd HH:mm:ss', '''','),', '''' ,' ${TIMEZONE_OFFSET}', '''',') AS etl_load_date') AS fields,
		CONCAT('CONCAT(','FORMAT(GETDATE(),','''','yyyy-MM-dd HH:mm:ss', '''','),', '''' ,' ${TIMEZONE_OFFSET}', '''',')') AS casting,
        'varchar(19)' AS field_type,
  		'{"name": "etl_load_date","type":["null", "string"], "default": null}' AS json,
        'etl_load_date' AS column_name,
        0 AS column_key,
		'' AS encoding
) x
ORDER BY x.ordinal_position
