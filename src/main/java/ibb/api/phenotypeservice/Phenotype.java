package ibb.api.phenotypeservice;

import java.util.List;

import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurationContext;
import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurer;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.hibernate.search.orm.elasticsearch.SearchExtension;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.groups.Default;

@Entity
@Indexed
public class Phenotype extends PanacheEntityBase {

    @SearchExtension
    public static class AnalysisConfigurer implements ElasticsearchAnalysisConfigurer {
        @Override
        public void configure(ElasticsearchAnalysisConfigurationContext context) {
            context.analyzer("english").custom()
                .tokenizer("standard")
                .tokenFilters("asciifolding", "lowercase", "porter_stem");
        }
    }

    public static interface ValidationGroups {
        public static interface Create extends Default {}
        public static interface Update extends Default {}
    }

    @Embeddable
    public static class DsRNA {
        @KeywordField
        @Schema(example = "iB_03553")
        public String name;

        @NotBlank
        @Schema(example = "CGAGATAGTGAGTTTCTTCT")
        public String sequence;
    }

    @Embeddable
    public static class OntologyTerm {
        @KeywordField
        @Schema(example = "TrOn_0000168")
        public String termId;
        
        @NotBlank
        @FullTextField(analyzer = "english")
        @Schema(example = "head")
        public String termName;
    }
    
    @Embeddable
    public static class Reference {
        public static enum Type {
            PMID, DOI, LAB
        }

        @Enumerated(EnumType.STRING)
        @Schema(example = "LAB")
        public Type type;

        @NotBlank
        @Schema(example = "Gregor Bucher")
        public String value;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Null(groups = ValidationGroups.Create.class)
    @NotNull(groups = ValidationGroups.Update.class)
    public Long id;

    @KeywordField
    @NotBlank
    @Schema(example = "TC003413")
    public String gene;

    @Valid
    @NotNull
    @Embedded
    @IndexedEmbedded
    @AttributeOverrides({
        @AttributeOverride(name = "name", column = @Column(name = "dsRNA_name")),
        @AttributeOverride(name = "sequence", column = @Column(name = "dsRNA_sequence"))
    })
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
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "type", column = @Column(name = "reference_type")),
        @AttributeOverride(name = "value", column = @Column(name = "reference_value"))
    })
    public Reference reference;

    @NotBlank
    @FullTextField(analyzer = "english")
    @Schema(example = "head size decreased")
    public String description;

    @NotNull
    @Valid
    @Embedded
    @IndexedEmbedded
    @AttributeOverrides({
        @AttributeOverride(name = "termId", column = @Column(name = "structure_termId")),
        @AttributeOverride(name = "termName", column = @Column(name = "structure_termName"))
    })
    public OntologyTerm structure;

    @FullTextField(analyzer = "english")
    @Schema(example = "head development")
    public String process;

    @NotNull
    @Schema(example = "0.6")
    public Float penetrance;

    @Schema(example = "10")
    public Integer numberOfAnimals;

    @ElementCollection(fetch = FetchType.EAGER)
    public List<Long> imageIds;
}
