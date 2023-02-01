package org.eclipse.tractusx.ssi.extensions.wallet.vaultStorage;

import lombok.SneakyThrows;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.tractusx.ssi.extensions.core.setting.SsiSettings;
import org.eclipse.tractusx.ssi.spi.verifiable.credential.VerifiableCredential;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.InputStream;

import static org.mockito.Mockito.doReturn;

public class SsiVaultStorageWalletTest {

  private SsiSettings ssiSettings;
  private Vault vault;

  private SsiVaultStorageWallet ssiVaultStorageWallet;

  @BeforeEach
  public void setUp(){
    ssiSettings = Mockito.mock(SsiSettings.class);
    vault = Mockito.mock(Vault.class);
    ssiVaultStorageWallet = new SsiVaultStorageWallet(vault, ssiSettings);
  }

  @Test
  public void getMembershipCredentialSuccess(){
    // given
    String testVc = getTestMembershipVC();
    String alias = "someAliasForMembershipVC";
    doReturn(alias).when(ssiSettings).getMembershipVerifiableCredentialAlias();
    doReturn(testVc).when(vault).resolveSecret(alias);
    // when
    VerifiableCredential result = ssiVaultStorageWallet.getMembershipCredential();
    // then
    Assertions.assertTrue(result.toString().equals(testVc));
  }

  @SneakyThrows
  private String getTestMembershipVC(){
    final InputStream inputStream =
            SsiVaultStorageWalletTest.class.getClassLoader().getResourceAsStream("core/wallet/membership-credential.json");
    String vc = new String(inputStream.readAllBytes());
    return vc;
  }
}
