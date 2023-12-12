package ibb.api.phenotypeservice;

import java.util.List;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.groups.Default;

public class Phenotype {

    public static interface ValidationGroups {
        public static interface Create extends Default {}
        public static interface Update extends Default {}
    }

    public static class DsRNA {
        @Schema(example = "iB_03553")
        public String name;

        @NotBlank
        @Schema(example = "CGAGATAGTGAGTTTCTTCT")
        public String sequence;
    }

    public static class OntologyTerm {
        @Schema(example = "TrOn_0000168")
        public String termId;
        
        @NotBlank
        @Schema(example = "head")
        public String termName;
    }
    
    public static class Reference {
        public static enum Type {
            PMID, DOI, LAB
        }

        @Schema(example = "LAB")
        public Type type;

        @NotBlank
        @Schema(example = "Gregor Bucher")
        public String value;
    }

    @Null(groups = ValidationGroups.Create.class)
    @NotNull(groups = ValidationGroups.Update.class)
    public String id;

    @NotBlank
    @Schema(example = "TC003413")
    public String gene;

    @Valid
    @NotNull
    public DsRNA dsRNA;

    @NotNull
    @Schema(example = "0.5")
    public Float concentration; // ng/ul

    @NotBlank
    @Schema(example = "L1")
    public String injectedStage;

    @NotBlank
    @Schema(example = "Ga-2")
    public String injectedStrain;

    @NotNull
    @Valid
    public Reference reference;

    @NotBlank
    @Schema(example = "head size decreased")
    public String description;

    @NotNull
    @Valid
    public OntologyTerm structure;

    @Schema(example = "head development")
    public String process;

    @NotNull
    @Schema(example = "0.6")
    public Float penetrance;

    @Schema(example = "10")
    public Integer numberOfAnimals;

    public List<Long> imageIds;
}
