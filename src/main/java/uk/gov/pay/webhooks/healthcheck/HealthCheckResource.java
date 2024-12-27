package uk.gov.pay.webhooks.healthcheck;

import com.codahale.metrics.health.HealthCheck;
import com.google.common.collect.ImmutableMap;
import io.dropwizard.core.setup.Environment;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import java.util.Collection;
import java.util.Map;
import java.util.SortedMap;
import java.util.stream.Collectors;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.apache.commons.lang3.StringUtils.defaultString;

@Path("/")
public class HealthCheckResource {
    private static final Logger LOGGER = LoggerFactory.getLogger(HealthCheckResource.class);
    private final Environment environment;
    
    @Inject
    public HealthCheckResource(Environment environment) {
        this.environment = environment;
    }

    @GET
    @Path("healthcheck")
    @Produces(APPLICATION_JSON)
    @Operation(
            tags = "Other",
            summary = "Healthcheck endpoint for webhooks. Check database, deadlocks, hibernate and ping",
            responses = {
                    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(example = "{" +
                            "    \"database\": {" +
                            "        \"healthy\": true," +
                            "        \"message\": \"Healthy\"" +
                            "    }," +
                            "    \"ping\": {" +
                            "        \"healthy\": true," +
                            "        \"message\": \"Healthy\"" +
                            "    }," +
                            "    \"hibernate\": {" +
                            "        \"healthy\": true," +
                            "        \"message\": \"Healthy\"" +
                            "    }," +
                            "    \"deadlocks\": {" +
                            "        \"healthy\": true," +
                            "        \"message\": \"Healthy\"" +
                            "    }," +
                            "    \"sqsQueue\": {" +
                            "        \"healthy\": true," +
                            "        \"message\": \"Healthy\"" +
                            "    }" +
                            "}")), description = "OK"),
                    @ApiResponse(responseCode = "503", description = "Service unavailable. If any healthchecks fail")
            }
    )
    public Response healthCheck() {
        SortedMap<String, HealthCheck.Result> results = environment.healthChecks().runHealthChecks();

        Map<String, Map<String, Object>> response = results.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                                healthCheck -> ImmutableMap.of(
                                        "healthy", healthCheck.getValue().isHealthy(),
                                        "message", defaultString(healthCheck.getValue().getMessage(), "Healthy"))
                        )
                );

        if (allHealthy(results.values())) {
            return Response.ok(response).build();
        }
        LOGGER.error("Healthcheck Failure: {}", response);
        return Response.status(503).entity(response).build();
    }

    private boolean allHealthy(Collection<HealthCheck.Result> results) {
        return results.stream().allMatch(HealthCheck.Result::isHealthy);
    }
}
