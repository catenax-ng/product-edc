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

package net.catenax.edc.tests;

import io.cucumber.java.en.Given;
import java.util.stream.Stream;
import net.catenax.edc.tests.data.Policy;

public class PolicyStepDefs {

  @Given("'{connector}' has no policies")
  public void hasNoPolicies(Connector connector) throws Exception {

    final DataManagementAPI api = connector.getDataManagementAPI();

    Stream<Policy> policies = api.getAllPolicies();
    for (Policy policy : policies.toArray(Policy[]::new)) {
      api.deletePolicy(policy.getId());
    }
  }
}
