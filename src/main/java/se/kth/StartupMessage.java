package se.kth;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@ApplicationScoped
public class StartupMessage {

    private static final Logger LOG = Logger.getLogger(StartupMessage.class);

    @ConfigProperty(name = "app.mode")
    String mode;

    @ConfigProperty(name = "app.version")
    String appVersion;

    void onStart(@Observes StartupEvent event) {
        LOG.info("Application has started!");
        LOG.infof("MODE: %s", mode);
        LOG.infof("Application Version: %s", appVersion);
    }
}