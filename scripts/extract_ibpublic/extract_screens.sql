WITH AllScreen AS (
	SELECT
		DevelopmentalStage.name 	                    AS developmentalStage
		, CoreData.dsRNAID 		                        AS geneAlterationId
        , Screen.created                                AS createdAt
        , CASE
            WHEN DevelopmentalStage.name = 'egg' THEN 'empty'
            WHEN (Screen.created IS NULL OR Screen.created < '2013-10-01') THEN 'phase_1'
            WHEN Screen.created < '2016-05-13' THEN 'phase_2'
            ELSE 'phase_3'
        END                                             AS phase
	FROM Screen

	INNER JOIN CoreData
	ON CoreData.ID = Screen.coreData

	INNER JOIN ScreenPhase
	ON ScreenPhase.ID = Screen.phase

	INNER JOIN DevelopmentalStage
	ON DevelopmentalStage.ID = ScreenPhase.developmentalStageID
)

SELECT DISTINCT
	CONCAT(silencingSeqId, '_', developmentalStage, '_', phase)		AS id
    , developmentalStage
	, geneAlterationId
    , CONCAT(developmentalStage, '_', phase)                        AS experimentId
FROM AllScreen
;
