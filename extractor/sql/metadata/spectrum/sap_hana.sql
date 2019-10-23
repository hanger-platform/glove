SELECT * FROM (
    SELECT
        fields,
        casting,
		field_type,
        json,
        column_name,
        column_key
    FROM (

        SELECT
            -1 AS POSITION,
           	CASE
				WHEN '${PARTITION_TYPE}' = 'date' OR '${PARTITION_TYPE}' = 'timestamp' THEN 'TO_CHAR( TO_DATE( TO_BIGINT( CASE WHEN TO_BIGINT("' || COLUMN_NAME || '") = 0 THEN ''19000101'' ELSE "' || COLUMN_NAME || '" END ) ), ''${PARTITION_FORMAT}'' ) AS partition_field'
          		WHEN '${PARTITION_TYPE}' = 'id' THEN 'TO_BIGINT ( ( FLOOR( COALESCE( TO_BIGINT("' || COLUMN_NAME || '"), 1 ) / ( ${PARTITION_LENGTH} + 0.01 ) ) + 1 ) * ${PARTITION_LENGTH} ) AS partition_field'
           	END AS fields,
     		CASE
				WHEN '${PARTITION_TYPE}' = 'date' OR '${PARTITION_TYPE}' = 'timestamp' THEN 'TO_CHAR( TO_DATE( TO_BIGINT( CASE WHEN TO_BIGINT("' || COLUMN_NAME || '") = 0 THEN ''19000101'' ELSE "' || COLUMN_NAME || '" END ) ), ''${PARTITION_FORMAT}'' )'
          		WHEN '${PARTITION_TYPE}' = 'id' THEN 'TO_BIGINT ( ( FLOOR( COALESCE( TO_BIGINT("' || COLUMN_NAME || '"), 1 ) / ( ${PARTITION_LENGTH} + 0.01 ) ) + 1 ) * ${PARTITION_LENGTH} )'
           	END AS casting,
            'int' AS field_type,
			'{"name": "partition_field","type":["null", "int"], "default": null}' AS json,
            'partition_field' 							 AS column_name,
            0 											 AS column_key
        FROM
            TABLE_COLUMNS
        WHERE
            LOWER( SCHEMA_NAME ) = LOWER( REPLACE( '${INPUT_TABLE_SCHEMA}', '"', '' ) )
            AND
            LOWER( TABLE_NAME ) = LOWER( REPLACE( '${INPUT_TABLE_NAME}', '"', '' ) )
            AND
			LOWER( COLUMN_NAME ) = LOWER( REPLACE( '${PARTITION_FIELD}', '"', '' ) )

		UNION ALL

        SELECT
            0 AS POSITION,
            CASE
            	WHEN '${CUSTOM_PRIMARY_KEY}' != '' THEN '('||REPLACE('${CUSTOM_PRIMARY_KEY}',',','||')||') AS custom_primary_key'  ELSE CONCAT( STRING_AGG( '"' || dd03l.fieldname || '"', '||' ), ' AS custom_primary_key' )
            END AS fields,
            CASE
            	WHEN '${CUSTOM_PRIMARY_KEY}' != '' THEN '('||REPLACE('${CUSTOM_PRIMARY_KEY}',',','||' )||')' ELSE STRING_AGG( '"' || dd03l.fieldname || '"', '||' )
            END AS casting,
            'varchar(255)' AS field_type,
            '{"name": "custom_primary_key","type":["null", "string"], "default": null}' AS json,
            'custom_primary_key' AS column_name,
            1 AS column_key
        FROM
            ${INPUT_TABLE_SCHEMA}.dd03l
        WHERE
            LOWER( dd03l.tabname ) = LOWER( REPLACE( '${INPUT_TABLE_NAME}', '"', '' ) )
            AND
            dd03l.keyflag = 'X'
        GROUP BY dd03l.tabname

        UNION ALL

        SELECT DISTINCT
            POSITION,
            CASE
                WHEN DATA_TYPE_NAME = 'DATE' THEN 'TO_CHAR ( ' || COLUMN_NAME || ' , ' || '''' || 'YYYY-MM-DD' ||  '''' || ') AS ' || REPLACE_REGEXPR( '\/\w+\/' IN COLUMN_NAME WITH '' )
                WHEN DATA_TYPE_NAME IN  ( 'TIME', 'SECONDDATE', 'TIMESTAMP' ) THEN 'CONCAT( TO_CHAR ( ' || COLUMN_NAME || ' , ' || '''' || 'YYYY-MM-DD HH24:MI:SS' || '''' || '),' || '''' || ' ${TIMEZONE_OFFSET}' || '''' || ') AS ' || REPLACE_REGEXPR( '\/\w+\/' IN COLUMN_NAME WITH '' )
                WHEN DATA_TYPE_NAME in ( 'VARCHAR', 'NVARCHAR', 'ALPHANUM', 'SHORTTEXT', 'BLOB', 'CLOB', 'NCLOB', 'TEXT' ) then '"' || COLUMN_NAME || '"' || ' as ' || REPLACE_REGEXPR( '\/\w+\/' IN COLUMN_NAME WITH '' )
                WHEN DATA_TYPE_NAME in ( 'VARBINARY' ) then 'TO_NVARCHAR (' || COLUMN_NAME || ')' || ' as ' || REPLACE_REGEXPR( '\/\w+\/' IN COLUMN_NAME WITH '' )
                ELSE '"' || COLUMN_NAME || '"' || ' AS ' || REPLACE_REGEXPR( '\/\w+\/' IN COLUMN_NAME WITH '' )
            END AS fields,
            CASE
                WHEN DATA_TYPE_NAME = 'DATE' THEN 'TO_CHAR ( ' || COLUMN_NAME || ' , ' || '''' || 'YYYY-MM-DD' ||  '''' || ')'
                WHEN DATA_TYPE_NAME IN  ( 'TIME', 'SECONDDATE', 'TIMESTAMP' ) THEN 'CONCAT( TO_CHAR ( ' || COLUMN_NAME || ' , ' || '''' || 'YYYY-MM-DD HH24:MI:SS' || '''' || '),' || '''' || ' ${TIMEZONE_OFFSET}' || '''' || ')'
                WHEN DATA_TYPE_NAME IN ( 'VARCHAR', 'NVARCHAR', 'ALPHANUM', 'SHORTTEXT', 'BLOB', 'CLOB', 'NCLOB', 'TEXT' ) then '"' || COLUMN_NAME || '"'
                WHEN DATA_TYPE_NAME in ( 'VARBINARY' ) then 'TO_NVARCHAR (' || COLUMN_NAME || ')'
                ELSE '"' || COLUMN_NAME || '"'
            END AS casting,
			CASE DATA_TYPE_NAME
				WHEN 'TINYINT'		THEN 'int'
				WHEN 'SMALLINT'		THEN 'int'
				WHEN 'INTEGER' 		THEN 'int'
				WHEN 'BIGINT'		THEN 'bigint'
				WHEN 'VARCHAR'		THEN 'varchar'||'(' || TO_BIGINT( round(CASE WHEN MOD("LENGTH",2) = 0 THEN "LENGTH" * ("LENGTH" / 2) else ("LENGTH" + 1) * (("LENGTH" + 1)/ 2) end, 0)) ||')'
				WHEN 'VARBINARY'	THEN 'varchar'||'(' || TO_BIGINT( round(CASE WHEN MOD("LENGTH",2) = 0 THEN "LENGTH" * ("LENGTH" / 2) else ("LENGTH" + 1) * (("LENGTH" + 1)/ 2) end, 0)) ||')'
				WHEN 'NVARCHAR'		THEN 'varchar'||'(' || TO_BIGINT( round(CASE WHEN MOD("LENGTH",2) = 0 THEN "LENGTH" * ("LENGTH" / 2) else ("LENGTH" + 1) * (("LENGTH" + 1) /2) end, 0)) ||')'
				WHEN 'CHAR'			THEN 'varchar'||'(' || TO_BIGINT( round(CASE WHEN MOD("LENGTH",2) = 0 THEN "LENGTH" * ("LENGTH" / 2) else ("LENGTH" + 1) * (("LENGTH" + 1) /2) end, 0)) ||')'
				WHEN 'BLOB'			THEN 'varchar(65535)'
				WHEN 'CLOB'			THEN 'varchar(65535)'
				WHEN 'NCLOB'		THEN 'varchar(65535)'
				WHEN 'TEXT'			THEN 'varchar(65535)'
				WHEN 'REAL'			THEN CASE '${IS_SPECTRUM}' WHEN '1' THEN CASE '${HAS_ATHENA}' WHEN '1' THEN 'double' ELSE 'double precision' END ELSE 'double precision' END
				WHEN 'DOUBLE'		THEN CASE '${IS_SPECTRUM}' WHEN '1' THEN CASE '${HAS_ATHENA}' WHEN '1' THEN 'double' ELSE 'double precision' END ELSE 'double precision' END
                WHEN 'SMALLDECIMAL'	THEN CASE '${IS_SPECTRUM}' WHEN '1' THEN CASE '${HAS_ATHENA}' WHEN '1' THEN 'double' ELSE 'double precision' END ELSE 'double precision' END
				WHEN 'DECIMAL'		THEN CASE '${IS_SPECTRUM}' WHEN '1' THEN CASE '${HAS_ATHENA}' WHEN '1' THEN 'double' ELSE 'double precision' END ELSE 'double precision' END
				WHEN 'DATE'			THEN 'varchar(10)'
				WHEN 'TIME'			THEN 'varchar(19)'
				WHEN 'SECONDDATE'	THEN 'varchar(19)'
				WHEN 'TIMESTAMP'	THEN 'varchar(19)'
				WHEN 'BOOLEAN' 		THEN 'boolean'
			END	AS field_type,
            ( '{"name": "' || LOWER( REPLACE_REGEXPR( '\/\w+\/' IN COLUMN_NAME WITH '' ) ) ||  '","type":' ||
                CASE
                	  WHEN DATA_TYPE_NAME IN ( 'TINYINT', 'SMALLINT', 'INTEGER' ) THEN '["null", "int"]'
                    WHEN DATA_TYPE_NAME IN ( 'BIGINT' ) THEN '["null", "long"]'
                    WHEN DATA_TYPE_NAME IN ( 'VARCHAR', 'VARBINARY', 'NVARCHAR', 'CHAR', 'BLOB', 'CLOB', 'NCLOB', 'TEXT' ) THEN '["null", "string"]'
                    WHEN DATA_TYPE_NAME IN ( 'REAL', 'DOUBLE', 'SMALLDECIMAL', 'DECIMAL' ) THEN '["null", "double"]'
                    WHEN DATA_TYPE_NAME IN ( 'DATE', 'TIME', 'SECONDDATE', 'TIMESTAMP' ) THEN '["null", "string"]'
                	  WHEN DATA_TYPE_NAME = 'BOOLEAN' THEN '["null", "boolean"]'
                end ||', "default": null}' ) AS json,
            LOWER( REPLACE_REGEXPR( '\/\w+\/' IN COLUMN_NAME WITH '' ) ) AS column_name,
            0 AS column_key
        FROM
            TABLE_COLUMNS
        WHERE
            LOWER( SCHEMA_NAME ) = LOWER( REPLACE( '${INPUT_TABLE_SCHEMA}', '"', '' ) )
            and
            LOWER( TABLE_NAME ) = LOWER( REPLACE( '${INPUT_TABLE_NAME}', '"', '' ) )

        UNION ALL

        SELECT
            998 AS POSITION,
			'CONCAT( TO_CHAR( now(),' || '''' || 'YYYY-MM-DD HH24:MI:SS' || '''' || '),' || '''' || ' ${TIMEZONE_OFFSET}' || '''' || ') as etl_load_date' as fields,
            'CONCAT( TO_CHAR( now(),' || '''' || 'YYYY-MM-DD HH24:MI:SS' || '''' || '),' || '''' || ' ${TIMEZONE_OFFSET}' || '''' || ')' AS casting,
            'varchar(19)' AS field_type,
            '{"name": "etl_load_date","type":["null", "string"], "default": null}' AS json,
            'etl_load_date' AS column_name,
            0 AS column_key
        FROM dummy
    )x ORDER BY x.POSITION
)
