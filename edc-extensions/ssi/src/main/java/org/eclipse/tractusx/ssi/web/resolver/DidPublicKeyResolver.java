/*
 *  Copyright (c) 2022 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial implementation
 *
 */

package org.eclipse.tractusx.ssi.web.resolver;

import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;

import java.security.PublicKey;

/**
 * Resolves a public key contained in a DID document associated with a DID.
 */
@ExtensionPoint
public interface DidPublicKeyResolver {

    /**
     * Resolves the public key.
     */
    PublicKey resolvePublicKey(String did);

}
