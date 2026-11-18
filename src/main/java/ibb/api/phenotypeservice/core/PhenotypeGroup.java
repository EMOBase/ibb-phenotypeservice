package ibb.api.phenotypeservice.core;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.quarkus.runtime.annotations.RegisterForReflection;

// The Elasticsearch client maps documents to this class by reflection, which
// a native executable only allows for registered classes.
@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_ABSENT)
public class PhenotypeGroup {
    public String gene;
    public List<Phenotype> phenotypes;
}
