package org.eclipse.dataspaceconnector.core.security.hashicorp;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.cert.X509Certificate;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMDecryptorProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@UtilityClass
final class PemUtil {
  private static final Provider PROVIDER = new BouncyCastleProvider();
  private static final JcaX509CertificateConverter X509_CONVERTER =
      new JcaX509CertificateConverter().setProvider(PROVIDER);
  private static final JcaPEMKeyConverter KEY_CONVERTER =
      new JcaPEMKeyConverter().setProvider(PROVIDER);

  @SneakyThrows
  public static X509Certificate readX509Certificate(@NotNull @NonNull InputStream inputStream) {
    X509CertificateHolder x509CertificateHolder = parsePem(inputStream);
    if (x509CertificateHolder == null) {
      return null;
    }
    return X509_CONVERTER.getCertificate(x509CertificateHolder);
  }

  @SneakyThrows
  public static PrivateKey readPrivateKey(
      @NotNull @NonNull InputStream inputStream, @Nullable String password) {
    Object object = parsePem(inputStream);

    if (object == null) {
      return null;
    }

    KeyPair keyPair;
    if (object instanceof PEMEncryptedKeyPair) {
      PEMEncryptedKeyPair pemEncryptedKeyPair = (PEMEncryptedKeyPair) object;
      PEMDecryptorProvider pemDecryptorProvider =
          new JcePEMDecryptorProviderBuilder()
              .build(password != null ? password.toCharArray() : null);
      keyPair = KEY_CONVERTER.getKeyPair(pemEncryptedKeyPair.decryptKeyPair(pemDecryptorProvider));
    } else {
      PEMKeyPair pemKeyPair = (PEMKeyPair) object;
      keyPair = KEY_CONVERTER.getKeyPair(pemKeyPair);
    }

    return keyPair.getPrivate();
  }

  private static <T> T parsePem(@NotNull @NonNull InputStream inputStream) throws IOException {
    try (Reader reader = new InputStreamReader(inputStream)) {
      PEMParser pemParser = new PEMParser(reader);
      return (T) pemParser.readObject();
    }
  }
}
