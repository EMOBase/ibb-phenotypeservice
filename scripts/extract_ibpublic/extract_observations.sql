/**
 * Script to extract observations from ibpublic
 */

-- Function to remove HTML tags from texts
DROP FUNCTION IF EXISTS fnStripTags;
DELIMITER |
CREATE FUNCTION fnStripTags( Dirty varchar(4000) )
RETURNS varchar(4000)
DETERMINISTIC
BEGIN
	DECLARE iStart, iEnd, iLength int;

	SET Dirty = REPLACE(Dirty, CHAR(10), '');
	SET Dirty = REPLACE(Dirty, CHAR(13), '');
	SET Dirty = REPLACE(Dirty, '&nbsp;', ' ');
	SET Dirty = REPLACE(Dirty, '&amp;', '&');
	SET Dirty = REPLACE(Dirty, '&gt;', '>');
	SET Dirty = REPLACE(Dirty, '&lt;', '<');

    	WHILE Locate( '<', Dirty ) > 0 And Locate( '>', Dirty, Locate( '<', Dirty )) > 0 DO
      	BEGIN
        	SET iStart = Locate( '<', Dirty ), iEnd = Locate( '>', Dirty, Locate('<', Dirty ));
        	SET iLength = ( iEnd - iStart) + 1;
        	IF iLength > 0 THEN
          	BEGIN
            		SET Dirty = Insert( Dirty, iStart, iLength, '');
          	END;
        	END IF;
      	END;
    	END WHILE;
    	RETURN NULLIF(LTRIM(RTRIM(Dirty)), '');
END;
|
DELIMITER ;
-- END creating function


-- Table to store templateId-to-topic mappings
DROP TABLE IF EXISTS TMP_Observation_Topic;
SET GLOBAL local_infile=1;
CREATE TABLE TMP_Observation_Topic (
	topic_ID VARCHAR(100) NOT NULL,
	template_ID INT NOT NULL,
	PRIMARY KEY (topic_ID, template_ID)
);

LOAD DATA LOCAL INFILE "topic_template_observations.tsv"
INTO TABLE TMP_Observation_Topic
FIELDS TERMINATED BY '\t'
LINES TERMINATED BY '\n'
IGNORE 1 ROWS;
-- END creating table


-- Extracting useful data
WITH Observation AS (
	SELECT Feature.ID						                                            AS id
		, CoreData.dsRNAID					                                            AS silencingSeqId
		, DevelopmentalStage.name				                                        AS developmentalStage
        , Feature.templateID                                                            AS templateId
		, TMP_Observation_Topic.topic_ID				                                AS topicId
		, REGEXP_SUBSTR(ScreenPhase.displayName, '[0-9]+') 	                            AS dayPostInjection
		, Attribute.attributeName		                                                AS attr1
		, Attribute2.attributeName		                                                AS attr2
		, AttributeSpecifier.specifierName	                                            AS spec1
		, AttributeSpecifier2.specifierName	                                            AS spec2
		, (SELECT GROUP_CONCAT(DISTINCT qualifierName SEPARATOR ' ')
			FROM Qualifier
			INNER JOIN Feature_Qualifier
			ON Feature_Qualifier.selectedQualifier_ID = Qualifier.ID
			WHERE Feature_Qualifier.Feature_ID = Feature.ID
			AND Qualifier.qualifierName != 'NOT DEFINED'
            AND Qualifier.qualifierName != 'need term'
		)					                                                            AS qualifiers
		, (SELECT GROUP_CONCAT(DISTINCT modifierName SEPARATOR ' ')
			FROM Modifier
			INNER JOIN Feature_Modifier
			ON Feature_Modifier.selectedModifiers_ID = Modifier.ID
			WHERE Feature_Modifier.Feature_ID = Feature.ID
			AND Modifier.modifierName != 'NOT DEFINED'
            AND Modifier.modifierName != 'need term'
		)					                                                            AS modifiers
        , CASE
            WHEN penetranceAbsolute IS NOT NULL     THEN "ABSOLUTE"
            WHEN penetrancePercent IS NOT NULL      THEN "PERCENTAGE"
            ELSE "MISSING"
        END                                                                             AS penetranceType
        , IFNULL(IFNULL(penetrancePercent, penetranceAbsolute), 0)                      AS penetranceValue
        , fnStripTags(Comment.comment)                                                  AS comment
        , CASE
            WHEN DevelopmentalStage.name = 'egg'                                        THEN 'empty'
            WHEN (Screen.created IS NULL OR Screen.created < '2013-10-01')              THEN 'phase_1'
            WHEN Screen.created < '2016-05-13'                                          THEN 'phase_2'
            ELSE 'phase_3'
        END                                                                             AS phase

	FROM Feature

	INNER JOIN CoreData
	ON CoreData.ID = Feature.coreData

	INNER JOIN Screen
	ON Screen.ID = Feature.screen

	INNER JOIN ScreenPhase
	ON ScreenPhase.ID = Screen.phase

	INNER JOIN DevelopmentalStage
	ON DevelopmentalStage.ID = ScreenPhase.developmentalStageID

	LEFT JOIN Attribute
	ON Feature.attributeID = Attribute.ID

	LEFT JOIN Attribute AS Attribute2
	ON Feature.secAttributeID = Attribute2.ID

	LEFT JOIN AttributeSpecifier
	ON Feature.attributeSpecifierID = AttributeSpecifier.ID

	LEFT JOIN AttributeSpecifier AS AttributeSpecifier2
	ON Feature.secAttributeSpecifierID = AttributeSpecifier2.ID

    LEFT JOIN Comment
    ON Feature.comment_ID = Comment.ID

	LEFT JOIN TMP_Observation_Topic
	ON TMP_Observation_Topic.template_ID = Feature.templateID
)
-- END extracting data


-- Format output data
SELECT id
	, CONCAT(CONVERT(silencingSeqId USING utf8), '_', developmentalStage, '_', phase)   AS screenId
	, CONCAT(developmentalStage, '_', phase, '_', topicId)                              AS topicId
    , templateId
	, dayPostInjection
    , penetranceType
    , penetranceValue
	, CONCAT_WS(' ',
		attr1,
		attr2,
		spec1,
		spec2,
		qualifiers,
		modifiers
	)						                                                            AS description
    , comment

FROM Observation
;
-- END formatting


-- Cleaning up
DROP TABLE TMP_Observation_Topic;
DROP FUNCTION fnStripTags;
-- END cleaning up
