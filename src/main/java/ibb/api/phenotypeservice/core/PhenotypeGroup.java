package ibb.api.phenotypeservice.core;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_ABSENT)
public class PhenotypeGroup {
    public String gene;
    public List<Phenotype> phenotypes;
}
