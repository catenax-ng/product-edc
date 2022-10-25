/*
 *  Copyright (c) 2022 Fraunhofer Institute for Software and Systems Engineering
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

import java.util.Map;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.jwt.TokenValidationRule;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class IdsValidationRule implements TokenValidationRule {
  private static final String SECURITY_PROFILE = "securityProfile";
  private static final String ISSUER_CONNECTOR = "issuerConnector";
  private static final String REFERRING_CONNECTOR = "referringConnector";

  private final boolean validateReferring;

  public IdsValidationRule(boolean validateReferring) {
    this.validateReferring = validateReferring;
  }

  /** Validates the JWT by checking extended IDS rules. */
  @Override
  public Result<Void> checkRule(
      @NotNull ClaimToken toVerify, @Nullable Map<String, Object> additional) {
    if (additional != null) {
      var issuerConnector = additional.get(ISSUER_CONNECTOR);
      if (issuerConnector == null) {
        return Result.failure("Required issuerConnector is missing in message");
      }

      String securityProfile = null;
      if (additional.containsKey(SECURITY_PROFILE)) {
        securityProfile = additional.get(SECURITY_PROFILE).toString();
      }

      return verifyTokenIds(additional, issuerConnector.toString(), securityProfile);

    } else {
      throw new EdcException("Missing required additional information for IDS token validation");
    }
  }

  private Result<Void> verifyTokenIds(
      Map<String, Object> claims, String issuerConnector, @Nullable String securityProfile) {

    // referringConnector (DAT, optional) vs issuerConnector (Message-Header,
    // mandatory)
    var referringConnector = claims.get(REFERRING_CONNECTOR);

    if (validateReferring && !issuerConnector.equals(referringConnector)) {
      return Result.failure(
          "referringConnector in token does not match issuerConnector in message");
    }

    // securityProfile (DAT, mandatory) vs securityProfile (Message-Payload,
    // optional)
    try {
      var tokenSecurityProfile = claims.get(SECURITY_PROFILE);

      if (securityProfile != null && !securityProfile.equals(tokenSecurityProfile)) {
        return Result.failure("securityProfile in token does not match securityProfile in payload");
      }
    } catch (Exception e) {
      // Nothing to do, payload mostly no connector instance
    }

    return Result.success();
  }
}
