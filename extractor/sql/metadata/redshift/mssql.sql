SELECT * FROM (

	SELECT DISTINCT 
		0 AS ordinal_position,
		CASE WHEN '${CUSTOM_PRIMARY_KEY}'!= '' THEN CONCAT('CONCAT(','${CUSTOM_PRIMARY_KEY}',','''')',' AS custom_primary_key') ELSE CONCAT('CONCAT(', STRING_AGG(LOWER(column_name),','),','''')',' AS custom_primary_key') END AS fields,
		CASE WHEN '${CUSTOM_PRIMARY_KEY}'!= '' THEN CONCAT('CONCAT(','${CUSTOM_PRIMARY_KEY}',','''')') ELSE CONCAT('CONCAT(', STRING_AGG(LOWER(column_name),','),','''')') END AS casting,
		'varchar(255)' AS field_type,
		'' AS json,
		'custom_primary_key' AS column_name,
		1 AS column_key,
		'encode ${ENCODE}' AS encoding
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

	UNION ALL

	SELECT DISTINCT
		ordinal_position,
		CASE
			WHEN data_type IN ('datetime') THEN CONCAT('COALESCE(FORMAT(IIF(',column_name,' IS NULL', ',' , '''1900-01-01 00:00:00''', ',', column_name, '),', '''yyyy-MM-dd HH:mm:ss', ' ${TIMEZONE_OFFSET}', '''), ', '''1900-01-01 00:00:00''', ') AS ',column_name)
			WHEN data_type = 'date' THEN CONCAT('COALESCE(FORMAT(IIF(',column_name,' IS NULL', ',' , '''1900-01-01''', ',', column_name, '),', '''yyyy-MM-dd', '''), ', '''1900-01-01''', ') AS ',column_name)
			WHEN data_type IN ('bit','tinyint','smallint','int') THEN CONCAT('CAST(',column_name,' AS int) AS ',column_name)
			WHEN data_type IN ('bigint') THEN CONCAT('CAST(',column_name,' AS bigint) AS ',column_name)
			WHEN data_type IN ('text','varchar') then concat(column_name, ' AS ',column_name)
			ELSE column_name
		END AS fields,
		CASE
			WHEN data_type IN ('datetime') THEN CONCAT('COALESCE(FORMAT(IIF(',column_name,' IS NULL', ',' , '''1900-01-01 00:00:00''', ',', column_name, '),', '''yyyy-MM-dd HH:mm:ss', ' ${TIMEZONE_OFFSET}', '''), ', '''1900-01-01 00:00:00''', ')')
			WHEN data_type = 'date' THEN CONCAT('COALESCE(FORMAT(IIF(',column_name,' IS NULL', ',' , '''1900-01-01''', ',', column_name, '),', '''yyyy-MM-dd', '''), ', '''1900-01-01''', ')' )
			WHEN data_type IN ('bit','tinyint','smallint','int') THEN CONCAT('CAST(`',column_name,'` AS int)')
			WHEN data_type IN ('bigint') THEN CONCAT('CAST(',column_name,' AS bigint)')
			WHEN data_type IN ('text','varchar') then column_name
			ELSE column_name
		END AS casting,
			CASE data_type
			WHEN 'bit'      	THEN 'smallint'
			WHEN 'tinyint'      THEN 'smallint'
			WHEN 'smallint'     THEN 'smallint'
			WHEN 'int'          THEN 'int'
			WHEN 'bigint'       THEN 'bigint'
			WHEN 'text'         THEN 'varchar(65535)'
			WHEN 'image'        THEN 'varchar(65535)'
			WHEN 'xml'          THEN 'varchar(65535)'
			WHEN 'varbinary'    THEN 'varchar(65535)'			
			WHEN 'date'         THEN 'date'
			WHEN 'datetime'     THEN 'timestamp'
			WHEN 'time'         THEN 'timestamp'
			WHEN 'decimal'      THEN  CONCAT('decimal','(', IIF( NUMERIC_PRECISION > 38, 38, NUMERIC_PRECISION ) ,',',NUMERIC_SCALE,')')
			WHEN 'numeric'      THEN  CONCAT('numeric','(', IIF( NUMERIC_PRECISION > 38, 38, NUMERIC_PRECISION ) ,',',NUMERIC_SCALE,')')
			WHEN 'real'         THEN 'double precision'
			WHEN 'float'        THEN 'double precision'            
			WHEN 'char'         THEN CONCAT('varchar','(', CHARACTER_MAXIMUM_LENGTH + CEILING( ( CHARACTER_MAXIMUM_LENGTH - 1 ) / 2 ),')')
			WHEN 'varchar'      THEN CONCAT('varchar','(', CHARACTER_MAXIMUM_LENGTH + CEILING( ( CHARACTER_MAXIMUM_LENGTH - 1 ) / 2 ),')')
			WHEN 'nvarchar'     THEN CONCAT('varchar','(', CHARACTER_MAXIMUM_LENGTH + CEILING( ( CHARACTER_MAXIMUM_LENGTH - 1 ) / 2 ),')')
			ELSE 'varchar(255)'
		END AS field_type,
		'' AS json,
		LOWER( column_name ) AS column_name,
		0 AS column_key,
		'encode ${ENCODE}' AS encoding
	FROM
		information_schema.columns c
	WHERE
		LOWER( c.table_schema ) = LOWER('${INPUT_TABLE_SCHEMA}')
		AND
		LOWER( c.table_name ) = LOWER('${INPUT_TABLE_NAME}') 
		AND 
		UPPER(column_name) NOT IN (${METADATA_BLACKLIST})

	UNION ALL
	
	SELECT
        999 AS ordinal_position,
        CONCAT('CONCAT(','FORMAT(GETDATE(),','''','yyyy-MM-dd HH:mm:ss', '''','),', '''' ,' ${TIMEZONE_OFFSET}', '''',') AS etl_load_date') AS fields,
		CONCAT('CONCAT(','FORMAT(GETDATE(),','''','yyyy-MM-dd HH:mm:ss', '''','),', '''' ,' ${TIMEZONE_OFFSET}', '''',')') AS casting,
        'timestamp' AS field_type,
  		'' AS json,
        'etl_load_date' AS column_name,
        0 AS column_key,
		'encode ${ENCODE}' AS encoding 	
) x
ORDER BY x.ordinal_position
