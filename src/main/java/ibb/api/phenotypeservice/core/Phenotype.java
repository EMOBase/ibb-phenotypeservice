package ibb.api.phenotypeservice.core;

import java.time.Instant;
import java.util.List;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;

@JsonInclude(JsonInclude.Include.NON_ABSENT)
public class Phenotype {

    public static interface ValidationGroups {
        public static interface Create {}
    }

    public static class ImageMetadata {
        public static enum Status {
            PENDING, APPROVED
        }
        public String id;
        public Status status;
    }

    public static class DsRNA {
        @Schema(example = "iB_03553")
        public String name;

        @NotBlank(groups = ValidationGroups.Create.class)
        @Schema(example = "CGAGATAGTGAGTTTCTTCT")
        public String sequence;
    }

    public static class OntologyTerm {
        @Schema(example = "TrOn_0000168")
        public String termId;
        
        @NotBlank(groups = ValidationGroups.Create.class)
        @Schema(example = "head")
        public String termName;
    }
    
    public static class Reference {
        public static enum Type {
            PMID, DOI, LAB
        }

        @Schema(example = "LAB")
        public Type type;

        @NotBlank(groups = ValidationGroups.Create.class)
        @Schema(example = "Gregor Bucher")
        public String value;
    }

    @Null(groups = ValidationGroups.Create.class)
    public String id;

    @NotBlank(groups = ValidationGroups.Create.class)
    public String gene;

    @Valid
    @NotNull(groups = ValidationGroups.Create.class)
    public DsRNA dsRNA;

    @NotNull(groups = ValidationGroups.Create.class)
    @Schema(example = "0.5")
    public Float concentration; // ng/ul

    @NotBlank(groups = ValidationGroups.Create.class)
    @Schema(example = "L1")
    public String injectedStage;

    @NotBlank(groups = ValidationGroups.Create.class)
    @Schema(example = "Ga-2")
    public String injectedStrain;

    @NotNull(groups = ValidationGroups.Create.class)
    @Valid
    public Reference reference;

    @NotBlank(groups = ValidationGroups.Create.class)
    @Schema(example = "head size decreased")
    public String description;

    @NotEmpty(groups = ValidationGroups.Create.class)
    public List<@Valid OntologyTerm> structures;

    @Schema(example = "head development")
    public String process;

    @NotNull(groups = ValidationGroups.Create.class)
    @Schema(example = "0.6")
    public Float penetrance;

    @Schema(example = "10")
    public Integer numberOfAnimals;

    @Null(groups = ValidationGroups.Create.class)
    public List<String> imageIds;

    @Null(groups = ValidationGroups.Create.class)
    public List<ImageMetadata> images;

    public Integer dayPostInjection;
    public String comment;

    @Null(groups = ValidationGroups.Create.class)
    public String iBeetleTopic;

    @Null(groups = ValidationGroups.Create.class)
    public String iBeetleExperiment;

    @Null(groups = ValidationGroups.Create.class)
    public Instant createdAt;
}
