/*
 *  Copyright (c) 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.tractusx.ssi.web.resolver;

import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.tractusx.ssi.web.document.DidDocument;

/**
 * Delegates to a {@link DidResolver} to resolve a DID document.
 */
@ExtensionPoint
public interface DidResolverRegistry {

    /**
     * Registers a DID resolver.
     */
    void register(DidResolver resolver);

    /**
     * Resolves a DID document based on the DID method.
     */
    DidDocument resolve(String didKey);

}
