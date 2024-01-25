curl -X POST \
    -F 'phenotype=@example_phenotype.json' \
    -F 'images=@example_images/20608.jpeg' \
    -F 'images=@example_images/20609.jpeg' \
    -F 'images=@example_images/20610.jpeg' \
    localhost:8080/phenotypes
