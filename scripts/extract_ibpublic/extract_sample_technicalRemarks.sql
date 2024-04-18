/**
 * Script to extract technical remarks from ibpublic
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


-- Table to store templateId-to-groupId mappings
DROP TABLE IF EXISTS TMP_TechnicalRemark_Topic;
SET GLOBAL local_infile=1;
CREATE TABLE TMP_TechnicalRemark_Topic(
	topic_ID VARCHAR(100) NOT NULL,
	template_ID INT NOT NULL,
	PRIMARY KEY (topic_ID, template_ID)
);

LOAD DATA LOCAL INFILE "topic_template_technicalRemarks.tsv"
INTO TABLE TMP_TechnicalRemark_Topic
FIELDS TERMINATED BY '\t'
LINES TERMINATED BY '\n'
IGNORE 1 ROWS;
-- END creating table


-- Extracting useful data
WITH Remark AS (
	SELECT TechnicalRemark.ID					                                        AS id
        , templateID                                                                    AS templateId
		, CoreData.dsRNAID					                                            AS silencingSeqId
		, DevelopmentalStage.name				                                        AS developmentalStage
		, REGEXP_SUBSTR(ScreenPhase.displayName, '[0-9]+') 	                            AS dayPostInjection
		, fnStripTags(TechnicalRemark.remarkName)		                                AS name
		, fnStripTags(TechnicalRemarkBase.remark)		                                AS base1
		, (SELECT GROUP_CONCAT(remark SEPARATOR ', ')
			FROM TechnicalRemarkBase
			INNER JOIN TechnicalRemark_baseRemarkLists
			ON TechnicalRemark_baseRemarkLists.baseRemarks_ID = TechnicalRemarkBase.ID
			WHERE TechnicalRemark_baseRemarkLists.TechnicalRemark_ID = TechnicalRemark.ID
		) 							                                                    AS base2
		, fnStripTags(CONCAT_WS(' '
			, RemarkField.nummericalRemark
			, RemarkField.freeTextRemark
		))							                                                    AS value
		, fnStripTags(RemarkFieldLabel.label)			                                AS label
		, fnStripTags(Comment.comment)				                                    AS comment
		, TMP_TechnicalRemark_Topic.topic_ID				                            AS topicId
        , CASE
            WHEN DevelopmentalStage.name = 'egg'                                        THEN 'empty_egg'
            WHEN (CoreData.created IS NULL OR CoreData.created < '2013-10-01') AND DevelopmentalStage.name = 'larval' THEN 'larval_phase_1'
            WHEN (CoreData.created IS NULL OR CoreData.created < '2013-10-01') AND DevelopmentalStage.name = 'pupal'  THEN 'pupal_phase_1'
            WHEN CoreData.created < '2016-05-13' AND DevelopmentalStage.name = 'larval' THEN 'larval_phase_2'
            WHEN CoreData.created < '2016-05-13' AND DevelopmentalStage.name = 'pupal'  THEN 'pupal_phase_2'
            WHEN DevelopmentalStage.name = 'larval'                                     THEN 'larval_phase_3'
            WHEN DevelopmentalStage.name = 'pupal'                                      THEN 'pupal_phase_3'
            ELSE 'unknown'
        END                                                                             AS experimentId

	FROM TechnicalRemark

	INNER JOIN CoreData
	ON CoreData.ID = TechnicalRemark.coreData

	INNER JOIN ScreenPhase
	ON ScreenPhase.ID = TechnicalRemark.screenphase_ID

	INNER JOIN DevelopmentalStage
	ON DevelopmentalStage.ID = ScreenPhase.developmentalStageID

	LEFT JOIN TechnicalRemarkBase AS TechnicalRemarkBase
	ON TechnicalRemark.baseRemark_ID = TechnicalRemarkBase.ID

	LEFT JOIN Comment
	ON TechnicalRemark.comment_ID = Comment.ID

	LEFT JOIN TechnicalRemark_RemarkField
	ON TechnicalRemark_RemarkField.TechnicalRemark_ID = TechnicalRemark.ID

	LEFT JOIN RemarkField
	ON TechnicalRemark_RemarkField.remarkFields_ID = RemarkField.ID

	LEFT JOIN RemarkFieldLabel
	ON RemarkFieldLabel.ID = RemarkField.label_ID

    LEFT JOIN RemarkType
    ON TechnicalRemarkBase.remarkType_ID = RemarkType.ID

	LEFT JOIN TMP_TechnicalRemark_Topic
	ON TMP_TechnicalRemark_Topic.template_ID = TechnicalRemark.templateID

    WHERE CoreData.dsRNAID IN ('iB_00120', 'iB_00007')
)
-- END extracting data


-- Format output data
SELECT DISTINCT Remark.id
	, CONCAT(CONVERT(silencingSeqId USING utf8), '_', experimentId)             AS screenId
    , templateId
    , dayPostInjection
	, topicId
	, name
    , CONCAT_WS(', ', base1, base2, NULLIF(CONCAT_WS(' ', label, value), ''))           AS value
	, comment
FROM Remark

WHERE (base1 IS NOT NULL OR base2 IS NOT NULL OR value IS NOT NULL OR comment IS NOT NULL)
;
-- END formatting


-- Clean up
DROP TABLE TMP_TechnicalRemark_Topic;
DROP FUNCTION fnStripTags;
-- END cleaning up
