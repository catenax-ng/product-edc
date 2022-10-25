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
package org.eclipse.tractusx.edc.oauth2;

import java.net.URI;
import lombok.NonNull;
import lombok.Setter;
import okhttp3.OkHttpClient;
import org.eclipse.dataspaceconnector.iam.oauth2.spi.Oauth2JwtDecoratorRegistry;
import org.eclipse.dataspaceconnector.runtime.metamodel.annotation.Inject;
import org.eclipse.dataspaceconnector.runtime.metamodel.annotation.Provides;
import org.eclipse.dataspaceconnector.runtime.metamodel.annotation.Requires;
import org.eclipse.dataspaceconnector.runtime.metamodel.annotation.Setting;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.iam.IdentityService;
import org.eclipse.dataspaceconnector.spi.jwt.TokenGenerationService;
import org.eclipse.dataspaceconnector.spi.jwt.TokenValidationService;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

@Provides(IdentityService.class)
@Requires({
  OkHttpClient.class,
  Oauth2JwtDecoratorRegistry.class,
  TokenGenerationService.class,
  TokenValidationService.class
})
public class OAuth2Extension implements ServiceExtension {

  @Setting private static final String TOKEN_URL = "edc.oauth.token.url";

  @Setting private static final String PROVIDER_AUDIENCE = "edc.oauth.provider.audience";

  @Inject @Setter private OkHttpClient okHttpClient;

  @Inject @Setter private Oauth2JwtDecoratorRegistry jwtDecoratorRegistry;

  @Inject @Setter private TokenGenerationService tokenGenerationService;

  @Inject @Setter private TokenValidationService tokenValidationService;

  @Override
  public void initialize(@NonNull final ServiceExtensionContext serviceExtensionContext) {
    final String tokenUrl = serviceExtensionContext.getSetting(TOKEN_URL, null);
    if (tokenUrl == null) {
      throw new EdcException("Missing required setting: " + TOKEN_URL);
    }

    final URI tokenUri = URI.create(tokenUrl);

    final OAuth2IdentityService oAuth2IdentityService =
        new OAuth2IdentityService(
            tokenUri,
            okHttpClient,
            serviceExtensionContext.getTypeManager(),
            jwtDecoratorRegistry,
            tokenGenerationService,
            tokenValidationService);

    serviceExtensionContext.registerService(IdentityService.class, oAuth2IdentityService);
  }
}
