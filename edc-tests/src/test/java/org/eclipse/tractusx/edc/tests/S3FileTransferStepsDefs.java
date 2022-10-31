/*
 *  Copyright (c) 2022 ZF Friedrichshafen AG
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       ZF Friedrichshafen AG - Initial Implementation
 *
 */

package org.eclipse.tractusx.edc.tests;

import static org.awaitility.Awaitility.await;
import static org.eclipse.tractusx.edc.tests.NegotiationSteps.isNegotiationComplete;
import static org.junit.jupiter.api.Assertions.fail;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.AfterAll;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.eclipse.tractusx.edc.tests.data.Asset;
import org.eclipse.tractusx.edc.tests.data.AssetWithDataAddress;
import org.eclipse.tractusx.edc.tests.data.DataAddress;
import org.eclipse.tractusx.edc.tests.data.Permission;
import org.eclipse.tractusx.edc.tests.data.Policy;
import org.eclipse.tractusx.edc.tests.data.TransferProcessState;
import org.eclipse.tractusx.edc.tests.util.S3Client;
import org.eclipse.tractusx.edc.tests.util.Timeouts;
import org.junit.jupiter.api.Assertions;

public class S3FileTransferStepsDefs {

  String contractNegotiationId;
  String assetId;

  @Given("'{connector}' has an empty storage bucket called {string}")
  public void hasEmptyStorageBucket(Connector connector, String bucketName) {
    S3Client s3 = new S3Client(connector.getEnvironment());

    s3.createBucket(bucketName);

    Assertions.assertTrue(s3.listBuckets().contains(bucketName));
    Assertions.assertEquals(0, s3.listBucketContent(bucketName).size());
  }

  @Given("'{connector}' has a storage bucket called {string} with the file called {string}")
  public void hasAStorageBucketWithTheFile(Connector connector, String bucketName, String fileName)
      throws IOException {
    S3Client s3 = new S3Client(connector.getEnvironment());
    s3.createBucket(bucketName);
    s3.createFile(bucketName, fileName);

    Set<String> bucketContent = s3.listBucketContent(bucketName);

    Assertions.assertEquals(1, bucketContent.size());
    Assertions.assertTrue(bucketContent.contains(fileName));
  }

  @Given("'{connector}' has the following S3 assets")
  public void hasAssets(Connector connector, DataTable table) {
    final DataManagementAPI api = connector.getDataManagementAPI();

    parseDataTable(table)
        .forEach(
            asset -> {
              try {
                api.createAsset(asset);
              } catch (IOException e) {
                fail(e.getMessage());
              }
            });
  }

  @Then("'{connector}' negotiates the contract successfully with '{connector}'")
  public void negotiateContract(Connector sender, Connector receiver, DataTable dataTable)
      throws IOException {

    String definitionId = dataTable.asMaps().get(0).get("contract offer id");
    assetId = dataTable.asMaps().get(0).get("asset id");
    String policyId = dataTable.asMaps().get(0).get("policy id");

    final Policy policy =
        new Policy(policyId, List.of(new Permission("USE", null, new ArrayList<>())));

    final DataManagementAPI dataManagementAPI = sender.getDataManagementAPI();
    final String receiverIdsUrl = receiver.getEnvironment().getIdsUrl() + "/data";

    final String negotiationId =
        dataManagementAPI.initiateNegotiation(receiverIdsUrl, definitionId, assetId, policy);

    await()
        .pollDelay(Duration.ofMillis(500))
        .atMost(Timeouts.CONTRACT_NEGOTIATION)
        .until(() -> isNegotiationComplete(dataManagementAPI, negotiationId));

    contractNegotiationId = dataManagementAPI.getNegotiation(negotiationId).getAgreementId();
  }

  @Then("'{connector}' initiate transfer process from '{connector}'")
  public void initiateTransferProcess(Connector sender, Connector receiver, DataTable dataTable)
      throws IOException {
    DataAddress dataAddress = createDataAddress(dataTable.asMaps().get(0));

    final DataManagementAPI dataManagementAPI = sender.getDataManagementAPI();
    final String receiverIdsUrl = receiver.getEnvironment().getIdsUrl() + "/data";

    final String transferProcessId =
        dataManagementAPI.initiateTransferProcess(
            receiverIdsUrl, contractNegotiationId, assetId, dataAddress);

    await()
        .pollDelay(Duration.ofMillis(500))
        .atMost(Timeouts.FILE_TRANSFER)
        .until(() -> isTransferComplete(dataManagementAPI, transferProcessId));

    Assertions.assertNotNull(transferProcessId);
  }

  private static final String COMPLETION_MARKER = ".complete";

  @Then("'{connector}' has a storage bucket called {string} with transferred file called {string}")
  public void consumerHasAStorageBucketWithFileTransferred(
      Connector connector, String bucketName, String fileName) {
    S3Client s3 = new S3Client(connector.getEnvironment());

    await()
        .pollDelay(Duration.ofMillis(500))
        .atMost(Timeouts.FILE_TRANSFER)
        .until(() -> isFilePresent(s3, bucketName, fileName + COMPLETION_MARKER));

    Set<String> bucketContent = s3.listBucketContent(bucketName);

    Assertions.assertEquals(2, bucketContent.size());
    Assertions.assertTrue(bucketContent.contains(fileName));
  }

  private boolean isFilePresent(S3Client s3, String bucketName, String fileName) {
    return s3.listBucketContent(bucketName).contains(fileName);
  }

  private List<AssetWithDataAddress> parseDataTable(DataTable table) {
    final List<AssetWithDataAddress> assetsWithDataAddresses = new ArrayList<>();

    for (Map<String, String> map : table.asMaps()) {
      String id = map.get("id");
      String description = map.get("description");
      assetsWithDataAddresses.add(
          new AssetWithDataAddress(new Asset(id, description), createDataAddress(map)));
    }

    return assetsWithDataAddresses;
  }

  private DataAddress createDataAddress(Map<String, String> map) {
    Map<String, Object> properties = new HashMap<>();
    properties.put("type", map.get("data_address_type"));
    properties.put("bucketName", map.get("data_address_s3_bucket_name"));
    properties.put("region", map.get("data_address_s3_region"));
    properties.put("keyName", map.get("data_address_s3_key_name"));
    return new DataAddress(properties);
  }

  private static boolean isTransferComplete(
      DataManagementAPI dataManagementAPI, String transferProcessId) throws IOException {
    var transferProcess = dataManagementAPI.getTransferProcess(transferProcessId);
    return transferProcess != null
        && transferProcess.getState().equals(TransferProcessState.COMPLETED);
  }

  @AfterAll
  public static void bucketsCleanup() {
    S3Client s3 = new S3Client(Environment.byName("Plato"));
    s3.deleteAllBuckets();
  }
}
