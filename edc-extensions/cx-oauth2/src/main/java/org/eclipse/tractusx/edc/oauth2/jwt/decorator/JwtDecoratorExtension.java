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
package org.eclipse.tractusx.edc.oauth2.jwt.decorator;

import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.NonNull;
import lombok.Setter;
import org.eclipse.dataspaceconnector.iam.oauth2.spi.Oauth2JwtDecoratorRegistry;
import org.eclipse.dataspaceconnector.runtime.metamodel.annotation.Inject;
import org.eclipse.dataspaceconnector.runtime.metamodel.annotation.Provides;
import org.eclipse.dataspaceconnector.runtime.metamodel.annotation.Requires;
import org.eclipse.dataspaceconnector.runtime.metamodel.annotation.Setting;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.security.CertificateResolver;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

@Provides(Oauth2JwtDecoratorRegistry.class)
@Requires(CertificateResolver.class)
public class JwtDecoratorExtension implements ServiceExtension {

  @Setting
  private static final String TOKEN_EXPIRATION_SECONDS = "edc.oauth.token.expiration.seconds";

  private static final Duration DEFAULT_EXPIRATION = Duration.ofMinutes(5);

  @Setting private static final String PUBLIC_KEY_ALIAS = "edc.oauth.public.key.alias";

  @Setting private static final String CLIENT_ID = "edc.oauth.client.id";

  @Inject @Setter private CertificateResolver certificateResolver;

  @Override
  public void initialize(@NonNull final ServiceExtensionContext serviceExtensionContext) {
    final Oauth2JwtDecoratorRegistry oauth2JwtDecoratorRegistry =
        new Oauth2JwtDecoratorRegistryRegistryImpl();

    Stream.of(
            audJwtDecorator(),
            expJwtDecorator(serviceExtensionContext),
            iatJwtDecorator(serviceExtensionContext),
            issJwtDecorator(serviceExtensionContext),
            jtiJwtDecorator(),
            subJwtDecorator(serviceExtensionContext),
            x5tJwtDecorator(serviceExtensionContext, certificateResolver),
            dapsJwtDecorator())
        .forEach(oauth2JwtDecoratorRegistry::register);

    serviceExtensionContext.registerService(
        Oauth2JwtDecoratorRegistry.class, oauth2JwtDecoratorRegistry);
  }

  private DapsJwtDecorator dapsJwtDecorator() {
    return new DapsJwtDecorator();
  }

  private IdsAudJwtDecorator audJwtDecorator() {
    return new IdsAudJwtDecorator();
  }

  private ExpJwtDecorator expJwtDecorator(
      @NonNull final ServiceExtensionContext serviceExtensionContext) {
    final Duration expiration =
        Duration.ofSeconds(
            serviceExtensionContext
                .getConfig()
                .getLong(TOKEN_EXPIRATION_SECONDS, DEFAULT_EXPIRATION.toSeconds()));

    return new ExpJwtDecorator(serviceExtensionContext.getClock(), expiration);
  }

  private IatJwtDecorator iatJwtDecorator(
      @NonNull final ServiceExtensionContext serviceExtensionContext) {
    return new IatJwtDecorator(serviceExtensionContext.getClock());
  }

  private IssJwtDecorator issJwtDecorator(
      @NonNull final ServiceExtensionContext serviceExtensionContext) {
    final String issuer = serviceExtensionContext.getConfig().getString(CLIENT_ID);

    return new IssJwtDecorator(issuer);
  }

  private JtiJwtDecorator jtiJwtDecorator() {
    return new JtiJwtDecorator();
  }

  private SubJwtDecorator subJwtDecorator(
      @NonNull final ServiceExtensionContext serviceExtensionContext) {
    final String subject = serviceExtensionContext.getConfig().getString(CLIENT_ID);

    return new SubJwtDecorator(subject);
  }

  private X5tJwtDecorator x5tJwtDecorator(
      @NonNull final ServiceExtensionContext serviceExtensionContext,
      @NonNull final CertificateResolver certificateResolver) {
    final String publicKeyAlias = serviceExtensionContext.getSetting(PUBLIC_KEY_ALIAS, null);
    if (publicKeyAlias == null) {
      throw new EdcException("Missing required setting: " + PUBLIC_KEY_ALIAS);
    }

    final X509Certificate certificate =
        Optional.ofNullable(certificateResolver.resolveCertificate(publicKeyAlias))
            .orElseThrow(
                () ->
                    new EdcException(
                        String.format("Public certificate not found: %s", publicKeyAlias)));

    final byte[] encodedCertificate;
    try {
      encodedCertificate = certificate.getEncoded();
    } catch (final CertificateEncodingException certificateEncodingException) {
      throw new EdcException(
          "Failed to encode certificate: " + certificateEncodingException.getMessage(),
          certificateEncodingException);
    }

    return new X5tJwtDecorator(encodedCertificate);
  }
}
