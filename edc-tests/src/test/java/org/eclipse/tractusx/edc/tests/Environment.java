/*
 *  Copyright (c) 2022 Mercedes-Benz Tech Innovation GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Mercedes-Benz Tech Innovation GmbH - Initial API and Implementation
 *       ZF Friedrichshafen AG - add support for AWS endpoint override
 *
 */

package org.eclipse.tractusx.edc.tests;

import static org.eclipse.tractusx.edc.tests.Constants.BACKEND_SERVICE_BACKEND_API_URL;
import static org.eclipse.tractusx.edc.tests.Constants.DATABASE_PASSWORD;
import static org.eclipse.tractusx.edc.tests.Constants.DATABASE_URL;
import static org.eclipse.tractusx.edc.tests.Constants.DATABASE_USER;
import static org.eclipse.tractusx.edc.tests.Constants.DATA_MANAGEMENT_API_AUTH_KEY;
import static org.eclipse.tractusx.edc.tests.Constants.DATA_MANAGEMENT_URL;
import static org.eclipse.tractusx.edc.tests.Constants.DATA_PLANE_URL;
import static org.eclipse.tractusx.edc.tests.Constants.EDC_AWS_ENDPOINT_OVERRIDE;
import static org.eclipse.tractusx.edc.tests.Constants.IDS_URL;

import java.util.Locale;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

@Builder(access = AccessLevel.PRIVATE)
@Getter
public class Environment {
  @NonNull private final String dataManagementAuthKey;
  @NonNull private final String dataManagementUrl;
  @NonNull private final String idsUrl;
  @NonNull private final String dataPlaneUrl;
  @NonNull private final String backendServiceBackendApiUrl;
  @NonNull private final String databaseUrl;
  @NonNull private final String databaseUser;
  @NonNull private final String databasePassword;
  @NonNull private final String awsEndpointOverride;

  public static Environment byName(String name) {
    name = name.toUpperCase(Locale.ROOT);

    return Environment.builder()
        .dataManagementUrl(System.getenv(String.join("_", name, DATA_MANAGEMENT_URL)))
        .dataManagementAuthKey(System.getenv(String.join("_", name, DATA_MANAGEMENT_API_AUTH_KEY)))
        .idsUrl(System.getenv(String.join("_", name, IDS_URL)))
        .dataPlaneUrl(System.getenv(String.join("_", name, DATA_PLANE_URL)))
        .backendServiceBackendApiUrl(
            System.getenv(String.join("_", name, BACKEND_SERVICE_BACKEND_API_URL)))
        .databaseUrl(System.getenv(String.join("_", name, DATABASE_URL)))
        .databaseUser(System.getenv(String.join("_", name, DATABASE_USER)))
        .databasePassword(System.getenv(String.join("_", name, DATABASE_PASSWORD)))
        .awsEndpointOverride(System.getenv(EDC_AWS_ENDPOINT_OVERRIDE))
        .build();
  }
}
