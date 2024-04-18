WITH AnatomicalFeature AS (
    SELECT * FROM Feature
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

, OntoMappingWithPantagmatic AS (
    SELECT ontologyID, attribute_ID, devStage_ID, secondAttribute_ID FROM OntoMapping
    UNION
    SELECT ontologyID, 14, devStage_ID, secondAttribute_ID FROM OntoMapping WHERE attribute_ID IN (10, 11) -- head & thorax
    UNION
    SELECT ontologyID, 15, devStage_ID, secondAttribute_ID FROM OntoMapping WHERE attribute_ID IN (11, 12) -- thorax & abdomen
    UNION
    SELECT ontologyID, 16, devStage_ID, secondAttribute_ID FROM OntoMapping WHERE attribute_ID IN (12, 13) -- abdomen & terminus
    UNION
    SELECT ontologyID, 17, devStage_ID, secondAttribute_ID FROM OntoMapping WHERE attribute_ID IN (11, 12, 13) -- thorax & abdomen & terminus
    UNION
    SELECT ontologyID, 18, devStage_ID, secondAttribute_ID FROM OntoMapping WHERE attribute_ID IN (10, 11, 12) -- head & thorax & abdomen
)

, BestMappedFeature AS (
    SELECT DISTINCT
        AnatomicalFeature.ID                            AS featureId
        , OntoMappingWithPantagmatic.ontologyID         AS tronId
        , AnatomicalFeature.attributeID                 AS attrId1
        , AnatomicalFeature.secAttributeID              AS attrId2
    FROM AnatomicalFeature
    INNER JOIN OntoMappingWithPantagmatic
    ON AnatomicalFeature.devStageSubdiv_ID = OntoMappingWithPantagmatic.devStage_ID

    WHERE (AnatomicalFeature.attributeID = OntoMappingWithPantagmatic.attribute_ID
        OR AnatomicalFeature.attributeID IS NULL AND OntoMappingWithPantagmatic.attribute_ID IS NULL)

    AND (AnatomicalFeature.secAttributeID = OntoMappingWithPantagmatic.secondAttribute_ID
        OR AnatomicalFeature.secAttributeID IS NULL AND OntoMappingWithPantagmatic.secondAttribute_ID IS NULL)
)

, GoodMappedFeature AS (
    SELECT DISTINCT
        AnatomicalFeature.ID                            AS featureId
        , OntoMappingWithPantagmatic.ontologyID         AS tronId
        , AnatomicalFeature.attributeID                 AS attrId1
        , AnatomicalFeature.secAttributeID              AS attrId2
    FROM AnatomicalFeature
    INNER JOIN OntoMappingWithPantagmatic
    ON AnatomicalFeature.devStageSubdiv_ID = OntoMappingWithPantagmatic.devStage_ID
    AND AnatomicalFeature.secAttributeID = OntoMappingWithPantagmatic.secondAttribute_ID
    WHERE OntoMappingWithPantagmatic.attribute_ID IS NULL
    AND AnatomicalFeature.ID NOT IN (
        SELECT featureId FROM BestMappedFeature
    )
)

, AcceptableMappedFeature AS (
    SELECT DISTINCT
        AnatomicalFeature.ID                            AS featureId
        , OntoMappingWithPantagmatic.ontologyID         AS tronId
        , AnatomicalFeature.attributeID                 AS attrId1
        , AnatomicalFeature.secAttributeID              AS attrId2
    FROM AnatomicalFeature
    INNER JOIN OntoMappingWithPantagmatic
    ON AnatomicalFeature.devStageSubdiv_ID = OntoMappingWithPantagmatic.devStage_ID
    AND AnatomicalFeature.secAttributeID = OntoMappingWithPantagmatic.secondAttribute_ID
    WHERE OntoMappingWithPantagmatic.attribute_ID IS NOT NULL
    AND AnatomicalFeature.ID NOT IN (
        SELECT featureId FROM BestMappedFeature
        UNION
        SELECT featureId FROM GoodMappedFeature
    )
)

, UnMappedFeature AS (
    SELECT DISTINCT
        AnatomicalFeature.ID                            AS featureId
        , NULL                                          AS tronId
        , AnatomicalFeature.attributeID                 AS attrId1
        , AnatomicalFeature.secAttributeID              AS attrId2
    FROM AnatomicalFeature

    WHERE AnatomicalFeature.ID NOT IN (
        SELECT featureId FROM BestMappedFeature
        UNION
        SELECT featureId FROM GoodMappedFeature
        UNION
        SELECT featureId FROM AcceptableMappedFeature
    )
)

, OutputFeature AS (
--    SELECT * FROM UnMappedFeature
    SELECT * FROM BestMappedFeature
    UNION
    SELECT * FROM GoodMappedFeature
    UNION
    SELECT * FROM AcceptableMappedFeature
)

, Output AS (
    SELECT OutputFeature.featureId                          AS observationId
        , OutputFeature.tronId                              AS ontologyTermId
        , CONCAT_WS(' ', Attribute.attributeName, Attribute2.attributeName) AS entity
        -- , Attribute.attributeName                           AS attr1
        -- , Attribute2.attributeName                          AS attr2
    FROM OutputFeature

    LEFT JOIN Attribute
    ON OutputFeature.attrId1 = Attribute.ID
    LEFT JOIN Attribute AS Attribute2
    ON OutputFeature.attrId2 = Attribute2.ID
)

SELECT * FROM Output
-- SELECT attr1, attr2, count(*) FROM Output GROUP BY attr1, attr2
;
