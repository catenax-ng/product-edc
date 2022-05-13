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
 *
 */

package org.eclipse.dataspaceconnector.core.security.hashicorp;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.time.Duration;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Builder
@Getter
@RequiredArgsConstructor
class HashicorpVaultClientConfig {
  private final String vaultUrl;
  private final String vaultToken;
  private final Duration timeout;
  private final X509Certificate certificate;
  private final X509Certificate certificateCa;
  private final PrivateKey certificatePrivateKey;
}
