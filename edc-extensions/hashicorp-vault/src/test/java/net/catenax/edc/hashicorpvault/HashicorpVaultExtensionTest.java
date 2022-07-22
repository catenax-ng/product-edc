package net.catenax.edc.hashicorpvault;

import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.system.health.HealthCheckService;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class HashicorpVaultExtensionTest {

  private static final String VAULT_URL = "https://example.com";
  private static final String VAULT_TOKEN = "token";

  private HashicorpVaultExtension extension;

  // mocks
  private ServiceExtensionContext context;
  private Monitor monitor;
  private HealthCheckService healthCheckService;

  @BeforeEach
  void setup() {
    context = Mockito.mock(ServiceExtensionContext.class);
    monitor = Mockito.mock(Monitor.class);
    healthCheckService = Mockito.mock(HealthCheckService.class);
    extension = new HashicorpVaultExtension();

    Mockito.when(context.getService(HealthCheckService.class)).thenReturn(healthCheckService);
    Mockito.when(context.getMonitor()).thenReturn(monitor);
    Mockito.when(context.getTypeManager()).thenReturn(new TypeManager());
    Mockito.when(context.getSetting(HashicorpVaultExtension.VAULT_URL, null)).thenReturn(VAULT_URL);
    Mockito.when(context.getSetting(HashicorpVaultExtension.VAULT_TOKEN, null))
        .thenReturn(VAULT_TOKEN);

    Mockito.when(
            context.getSetting(
                HashicorpVaultExtension.VAULT_API_SECRET_PATH,
                HashicorpVaultExtension.VAULT_API_SECRET_PATH_DEFAULT))
        .thenReturn(HashicorpVaultExtension.VAULT_API_SECRET_PATH_DEFAULT);
    Mockito.when(
            context.getSetting(
                HashicorpVaultExtension.VAULT_API_HEALTH_PATH,
                HashicorpVaultExtension.VAULT_API_HEALTH_PATH_DEFAULT))
        .thenReturn(HashicorpVaultExtension.VAULT_API_HEALTH_PATH_DEFAULT);
    Mockito.when(
            context.getSetting(
                HashicorpVaultExtension.VAULT_HEALTH_CHECK,
                HashicorpVaultExtension.VAULT_HEALTH_CHECK_DEFAULT))
        .thenReturn(HashicorpVaultExtension.VAULT_HEALTH_CHECK_DEFAULT);
    Mockito.when(
            context.getSetting(
                HashicorpVaultExtension.VAULT_HEALTH_CHECK_STANDBY_OK,
                HashicorpVaultExtension.VAULT_HEALTH_CHECK_STANDBY_OK_DEFAULT))
        .thenReturn(HashicorpVaultExtension.VAULT_HEALTH_CHECK_STANDBY_OK_DEFAULT);
  }

  @Test
  void registersHealthCheckIfEnabled() {
    Mockito.when(context.getSetting(HashicorpVaultExtension.VAULT_HEALTH_CHECK, true))
        .thenReturn(true);

    extension.initializeVault(context);

    Mockito.verify(healthCheckService, Mockito.times(1)).addReadinessProvider(Mockito.any());
    Mockito.verify(healthCheckService, Mockito.times(1)).addLivenessProvider(Mockito.any());
    Mockito.verify(healthCheckService, Mockito.times(1)).addStartupStatusProvider(Mockito.any());
  }

  @Test
  void registersNoHealthCheckIfDisabled() {
    Mockito.when(context.getSetting(HashicorpVaultExtension.VAULT_HEALTH_CHECK, true))
        .thenReturn(false);

    extension.initializeVault(context);

    Mockito.verify(healthCheckService, Mockito.times(0)).addReadinessProvider(Mockito.any());
    Mockito.verify(healthCheckService, Mockito.times(0)).addLivenessProvider(Mockito.any());
    Mockito.verify(healthCheckService, Mockito.times(0)).addStartupStatusProvider(Mockito.any());
  }
}
