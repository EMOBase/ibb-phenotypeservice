package ibb.api.phenotypeservice.models;

import java.util.List;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Entity
public class Phenotype extends PanacheEntity {

    @NotBlank
    public String gene;

    @Embedded
    @Valid
    public DsRNA dsRNA;

    @NotNull
    public Float concentration; // ng/ul

    @NotBlank
    public String injectedStage;

    @NotBlank
    public String injectedStrain;

    @Embedded
    @Valid
    public Reference reference;

    @NotBlank
    public String description;

    @Embedded
    @Valid
    public OntologyTerm structure;

    public String process;

    @NotNull
    public Float penetrance;

    public Integer numberOfAnimals;

    @ElementCollection
    public List<Long> imageIds;
}
