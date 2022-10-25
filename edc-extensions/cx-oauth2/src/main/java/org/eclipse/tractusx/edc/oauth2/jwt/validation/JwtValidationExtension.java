/*
 * Copyright (c) 2022 Mercedes-Benz Tech Innovation GmbH
 * Copyright (c) 2021,2022 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.eclipse.tractusx.edc.oauth2.jwt.validation;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.NonNull;
import lombok.Setter;
import okhttp3.OkHttpClient;
import org.eclipse.dataspaceconnector.iam.oauth2.spi.Oauth2ValidationRulesRegistry;
import org.eclipse.dataspaceconnector.runtime.metamodel.annotation.Inject;
import org.eclipse.dataspaceconnector.runtime.metamodel.annotation.Provides;
import org.eclipse.dataspaceconnector.runtime.metamodel.annotation.Requires;
import org.eclipse.dataspaceconnector.runtime.metamodel.annotation.Setting;
import org.eclipse.dataspaceconnector.spi.jwt.TokenValidationService;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.tractusx.edc.oauth2.jwk.JwkPublicKeyResolver;
import org.eclipse.tractusx.edc.oauth2.jwk.PublicKeyReader;
import org.eclipse.tractusx.edc.oauth2.jwk.RsaPublicKeyReader;

@Provides(TokenValidationService.class)
@Requires({OkHttpClient.class, Clock.class})
public class JwtValidationExtension implements ServiceExtension {

  @Setting private static final String EDC_IDS_ENDPOINT_AUDIENCE = "edc.ids.endpoint.audience";
  @Setting private static final String NOT_BEFORE_LEEWAY = "edc.oauth.validation.nbf.leeway";
  private static final Duration DEFAULT_NOT_BEFORE_LEEWAY = Duration.ofSeconds(10);

  @Setting
  private static final String PROVIDER_JWKS_REFRESH =
      "edc.oauth.provider.jwks.refresh"; // in minutes

  private static final Duration DEFAULT_PROVIDER_JWKS_REFRESH = Duration.ofMinutes(5);

  @Setting private static final String PROVIDER_JWKS_URL = "edc.oauth.provider.jwks.url";
  private static final String DEFAULT_JWKS_URL = "http://localhost/empty_jwks_url";

  @Setting
  public static final String EDC_IDS_VALIDATION_REFERRINGCONNECTOR =
      "edc.ids.validation.referringconnector";

  public static final boolean DEFAULT_EDC_IDS_VALIDATION_REFERRINGCONNECTOR = false;

  @Inject @Setter private OkHttpClient okHttpClient;

  @Inject @Setter private Clock clock;

  private JwkPublicKeyResolver jwkPublicKeyResolver;

  @Override
  public void initialize(@NonNull final ServiceExtensionContext serviceExtensionContext) {

    final Oauth2ValidationRulesRegistry oauth2ValidationRulesRegistry =
        oauth2ValidationRulesRegistry(serviceExtensionContext);

    this.jwkPublicKeyResolver = jwkPublicKeyResolver(serviceExtensionContext);

    final TokenValidationService tokenValidationService =
        new TokenValidationServiceImpl(jwkPublicKeyResolver, oauth2ValidationRulesRegistry);

    serviceExtensionContext.registerService(TokenValidationService.class, tokenValidationService);
  }

  @Override
  public void start() {
    Optional.ofNullable(jwkPublicKeyResolver).ifPresent(JwkPublicKeyResolver::start);
  }

  @Override
  public void shutdown() {
    Optional.ofNullable(jwkPublicKeyResolver).ifPresent(JwkPublicKeyResolver::stop);
  }

  private Oauth2ValidationRulesRegistry oauth2ValidationRulesRegistry(
      final ServiceExtensionContext serviceExtensionContext) {
    final Oauth2ValidationRulesRegistry oauth2ValidationRulesRegistry =
        new Oauth2ValidationRulesRegistryImpl();
    Stream.of(
            audValidationRule(serviceExtensionContext),
            expValidationRule(),
            iatValidationRule(),
            nbfValidationRule(serviceExtensionContext),
            idsValidationRule(serviceExtensionContext))
        .forEach(oauth2ValidationRulesRegistry::addRule);

    return oauth2ValidationRulesRegistry;
  }

  private JwkPublicKeyResolver jwkPublicKeyResolver(
      final ServiceExtensionContext serviceExtensionContext) {
    final URI jsonWebKeySetUri =
        URI.create(
            serviceExtensionContext.getConfig().getString(PROVIDER_JWKS_URL, DEFAULT_JWKS_URL));
    final Duration refreshInterval =
        Duration.ofMinutes(
            serviceExtensionContext
                .getConfig()
                .getLong(PROVIDER_JWKS_REFRESH, DEFAULT_PROVIDER_JWKS_REFRESH.toMinutes()));

    final RsaPublicKeyReader rsaPublicKeyReader =
        new RsaPublicKeyReader(serviceExtensionContext.getMonitor());
    final List<PublicKeyReader> publicKeyReaders = Collections.singletonList(rsaPublicKeyReader);

    return new JwkPublicKeyResolver(
        jsonWebKeySetUri,
        okHttpClient,
        serviceExtensionContext.getTypeManager(),
        serviceExtensionContext.getMonitor(),
        publicKeyReaders,
        refreshInterval);
  }

  private AudValidationRule audValidationRule(
      final ServiceExtensionContext serviceExtensionContext) {
    final String audience =
        Objects.requireNonNull(
            serviceExtensionContext.getConfig().getString(EDC_IDS_ENDPOINT_AUDIENCE));

    return new AudValidationRule(audience, serviceExtensionContext.getMonitor());
  }

  private ExpValidationRule expValidationRule() {
    return new ExpValidationRule(clock);
  }

  private IatValidationRule iatValidationRule() {
    return new IatValidationRule(clock);
  }

  private NbfValidationRule nbfValidationRule(
      final ServiceExtensionContext serviceExtensionContext) {
    final Duration nbfLeeway =
        Duration.ofSeconds(
            serviceExtensionContext
                .getConfig()
                .getLong(NOT_BEFORE_LEEWAY, DEFAULT_NOT_BEFORE_LEEWAY.toSeconds()));

    return new NbfValidationRule(nbfLeeway, clock);
  }

  private IdsValidationRule idsValidationRule(
      final ServiceExtensionContext serviceExtensionContext) {
    boolean validateReferring =
        serviceExtensionContext.getSetting(
            EDC_IDS_VALIDATION_REFERRINGCONNECTOR, DEFAULT_EDC_IDS_VALIDATION_REFERRINGCONNECTOR);

    return new IdsValidationRule(validateReferring);
  }
}
