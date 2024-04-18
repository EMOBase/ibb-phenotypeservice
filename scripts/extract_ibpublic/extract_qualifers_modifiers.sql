WITH AnatomicalFeature AS (
    SELECT DISTINCT
        Feature.ID                                          AS featureId
		, AttributeSpecifier.specifierName	                AS spec1
		, AttributeSpecifier2.specifierName	                AS spec2
		, (SELECT GROUP_CONCAT(DISTINCT qualifierName SEPARATOR ' ')
			FROM Qualifier
			INNER JOIN Feature_Qualifier
			ON Feature_Qualifier.selectedQualifier_ID = Qualifier.ID
			WHERE Feature_Qualifier.Feature_ID = Feature.ID
			AND Qualifier.qualifierName != 'NOT DEFINED'
            AND Qualifier.qualifierName != 'need term'
		)					                                AS qualifier
		, (SELECT GROUP_CONCAT(DISTINCT modifierName SEPARATOR ' ')
			FROM Modifier
			INNER JOIN Feature_Modifier
			ON Feature_Modifier.selectedModifiers_ID = Modifier.ID
			WHERE Feature_Modifier.Feature_ID = Feature.ID
			AND Modifier.modifierName != 'NOT DEFINED'
            AND Modifier.modifierName != 'need term'
		)					                                AS modifier

    FROM Feature

	LEFT JOIN AttributeSpecifier
	ON Feature.attributeSpecifierID = AttributeSpecifier.ID

	LEFT JOIN AttributeSpecifier AS AttributeSpecifier2
	ON Feature.secAttributeSpecifierID = AttributeSpecifier2.ID

    WHERE Feature.featureName IN (
        'Differentiated blastoderm'
        , 'adult abdomen'
        , 'adult head'
        , 'adult pantagmatic defects'
        , 'adult thorax'
        , 'egg morphology'
        , 'embryonic musculature'
        , 'empty eggs'
        , 'larval abdomen'
        , 'larval head'
        , 'larval pantagmatic defects'
        , 'larval thorax'
        , 'ovary phenotype (specific)'
        , 'pupal abdomen'
        , 'pupal head'
        , 'pupal pantagmatic defects'
        , 'pupal thoracic musculature'
        , 'pupal thorax'
        , 'stink gland phenotype'
        , 'strong defects'
        , 'strong phenotypes - description <b><font color="red">(tagmata/structures not mentioned are considered to be missing)</font></b>'
        , 'strong phenotypes - description <font color="red">(tagmata/structures not mentioned are considered to be missing)</font>'
    )
)

-- SELECT * FROM AnatomicalFeature

SELECT CONCAT_WS(' ', spec1, spec2, qualifier, modifier) AS quality
    , count(*)
FROM AnatomicalFeature
GROUP BY quality
ORDER BY count(*) DESC
;
