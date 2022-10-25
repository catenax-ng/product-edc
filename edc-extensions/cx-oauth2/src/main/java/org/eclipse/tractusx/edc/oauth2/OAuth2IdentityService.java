/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
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
import java.util.LinkedHashMap;
import java.util.Objects;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.iam.IdentityService;
import org.eclipse.dataspaceconnector.spi.iam.TokenParameters;
import org.eclipse.dataspaceconnector.spi.iam.TokenRepresentation;
import org.eclipse.dataspaceconnector.spi.jwt.JwtDecorator;
import org.eclipse.dataspaceconnector.spi.jwt.JwtDecoratorRegistry;
import org.eclipse.dataspaceconnector.spi.jwt.TokenGenerationService;
import org.eclipse.dataspaceconnector.spi.jwt.TokenValidationService;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;

@RequiredArgsConstructor
public class OAuth2IdentityService implements IdentityService {

  private static final String GRANT_TYPE_CLIENT_CREDENTIALS = "client_credentials";
  private static final String CLIENT_ASSERTION_TYPE_JWT_BEARER =
      "urn:ietf:params:oauth:client-assertion-type:jwt-bearer";
  private static final String CONTENT_TYPE_APPLICATION_FORM_URLENCODED =
      "application/x-www-form-urlencoded";
  private static final String CONTENT_TYPE = "Content-Type";
  private static final String RESOURCE = "resource";
  private static final String CLIENT_ASSERTION_TYPE = "client_assertion_type";
  private static final String GRANT_TYPE = "grant_type";
  private static final String CLIENT_ASSERTION = "client_assertion";
  private static final String SCOPE = "scope";

  @NonNull private final URI tokenUrl;
  @NonNull private final OkHttpClient httpClient;
  @NonNull private final TypeManager typeManager;
  @NonNull private final JwtDecoratorRegistry jwtDecoratorRegistry;
  @NonNull private final TokenGenerationService tokenGenerationService;
  @NonNull private final TokenValidationService tokenValidationService;

  @Override
  public Result<TokenRepresentation> obtainClientCredentials(
      @NonNull final TokenParameters tokenParameters) {
    final Result<TokenRepresentation> jwtCreationResult =
        tokenGenerationService.generate(jwtDecoratorRegistry.getAll().toArray(JwtDecorator[]::new));
    if (jwtCreationResult.failed()) {
      return jwtCreationResult;
    }

    final String assertion = jwtCreationResult.getContent().getToken();

    final FormBody.Builder requestBodyBuilder =
        new FormBody.Builder()
            .add(CLIENT_ASSERTION_TYPE, CLIENT_ASSERTION_TYPE_JWT_BEARER)
            .add(GRANT_TYPE, GRANT_TYPE_CLIENT_CREDENTIALS)
            .add(CLIENT_ASSERTION, assertion)
            .add(SCOPE, tokenParameters.getScope())
            .add(RESOURCE, tokenParameters.getAudience());

    try {
      final HttpUrl httpUrl = Objects.requireNonNull(HttpUrl.get(tokenUrl));
      final Request request =
          new Request.Builder()
              .url(httpUrl)
              .addHeader(CONTENT_TYPE, CONTENT_TYPE_APPLICATION_FORM_URLENCODED)
              .post(requestBodyBuilder.build())
              .build();

      try (final Response response = httpClient.newCall(request).execute()) {
        try (final ResponseBody responseBody = response.body()) {
          if (!response.isSuccessful()) {
            final String message = responseBody == null ? "<empty body>" : responseBody.string();
            return Result.failure(message);
          }

          if (responseBody == null) {
            return Result.failure("<empty token body>");
          }

          final String responsePayload = responseBody.string();

          @SuppressWarnings("rawtypes")
          LinkedHashMap deserialized = typeManager.readValue(responsePayload, LinkedHashMap.class);

          final String token = (String) deserialized.get("access_token");

          final TokenRepresentation tokenRepresentation =
              TokenRepresentation.Builder.newInstance().token(token).build();

          return Result.success(tokenRepresentation);
        }
      }
    } catch (final Exception exception) {
      throw new EdcException(exception);
    }
  }

  @Override
  public Result<ClaimToken> verifyJwtToken(
      @NonNull final TokenRepresentation tokenRepresentation, final String audience) {
    return tokenValidationService.validate(tokenRepresentation);
  }
}
