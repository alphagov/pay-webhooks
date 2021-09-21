package uk.gov.pay.webhooks.healthcheck;

import io.dropwizard.setup.Environment;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import java.util.Map;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("/")
public class HealthCheckResource {
    private final Environment environment;
    
    @Inject
    public HealthCheckResource(Environment environment) {
        this.environment = environment;
    }

    @GET
    @Path("healthcheck")
    @Produces(APPLICATION_JSON)
    public Response healthCheck() {
        var ok = Map.of("healthy", "true");
        return Response.ok(ok).build();
    }
}
