package ibb.api.phenotypeservice.rest;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.jboss.resteasy.reactive.RestPath;

import ibb.api.phenotypeservice.core.Lethality;
import ibb.api.phenotypeservice.core.LethalityRepository;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/lethality")
public class LethalityEndpoints {

    @Inject
    LethalityRepository lethalityRepo;

    @GET
    @Path("{ibNumber}")
    @Operation(summary = "Get death percentage on day 11 and 22 after injection for larvae and day 11 for pupae")
    @Parameter(name = "geneAlterationId", description = "iB number in format iB_[0-9]{5}", example = "iB_00002")
    public Lethality get(@RestPath String ibNumber) throws Exception {
        Lethality lethality = lethalityRepo.findByIbNumber(ibNumber);
        if (lethality == null) {
            return new Lethality();
        }
        return lethality;
    }
}
