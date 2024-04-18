WITH Observation AS (
	SELECT Feature.ID						                                            AS id
		, (SELECT GROUP_CONCAT(DISTINCT qualifierName SEPARATOR ' ')
			FROM Qualifier
			INNER JOIN Feature_Qualifier
			ON Feature_Qualifier.selectedQualifier_ID = Qualifier.ID
			WHERE Feature_Qualifier.Feature_ID = Feature.ID
			AND Qualifier.qualifierName NOT IN (
                'NOT DEFINED',
                'need term'
            )
		)					                                                            AS qualifiers
		, (SELECT GROUP_CONCAT(DISTINCT modifierName SEPARATOR ' ')
			FROM Modifier
			INNER JOIN Feature_Modifier
			ON Feature_Modifier.selectedModifiers_ID = Modifier.ID
			WHERE Feature_Modifier.Feature_ID = Feature.ID
			AND Modifier.modifierName NOT IN (
                'NOT DEFINED',
                'need term'
            )
		)					                                                            AS modifiers
	FROM Feature
)

SELECT id                                                                               AS observationId
	, CONCAT_WS(' ',
		qualifiers,
		modifiers
	)						                                                            AS ontologyTermId

FROM Observation
WHERE qualifiers != '' AND modifiers != ''
