package uk.gov.pay;

import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

public class pay-webhooksApplication extends Application<pay-webhooksConfiguration> {

    public static void main(final String[] args) throws Exception {
        new pay-webhooksApplication().run(args);
    }

    @Override
    public String getName() {
        return "pay-webhooks";
    }

    @Override
    public void initialize(final Bootstrap<pay-webhooksConfiguration> bootstrap) {
        // TODO: application initialization
    }

    @Override
    public void run(final pay-webhooksConfiguration configuration,
                    final Environment environment) {
        // TODO: implement application
    }

}
