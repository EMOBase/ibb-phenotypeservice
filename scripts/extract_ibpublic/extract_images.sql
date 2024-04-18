With Image AS (
    SELECT Feature.ID AS observationId
        , ImageHolder.ID AS imageId
    FROM Feature
    INNER JOIN ImageHolder
    ON Feature.ID = ImageHolder.feature_ID
)
SELECT * FROM Image;
