package org.eclipse.tractusx.ssi.verification;

import com.danubetech.verifiablecredentials.jwt.JwtVerifiablePresentation;
import com.nimbusds.jose.JOSEException;
import org.eclipse.tractusx.ssi.resolver.Did;
import org.eclipse.tractusx.ssi.resolver.DidPublicKeyResolver;
import org.eclipse.tractusx.ssi.util.DidParser;

import java.security.PublicKey;

public class VerifiablePresentationSignatureHandler implements VerifiablePresentationVerificationHandler {

    private final DidPublicKeyResolver didPublicKeyResolver;

    public VerifiablePresentationSignatureHandler(DidPublicKeyResolver didPublicKeyResolver) {
        this.didPublicKeyResolver = didPublicKeyResolver;
    }

    @Override
    public boolean canHandle(JwtVerifiablePresentation presentation) {
        return true;
    }

    @Override
    public boolean checkTrust(JwtVerifiablePresentation jwt) {

        final String subject = jwt.getPayload().getSubject(); // holder
        final Did holderDid = DidParser.parse(subject); // TODO might be a URI, handle this. Maybe check for DID:Web URI
        final PublicKey key = didPublicKeyResolver.resolve(holderDid);

        try {
            return jwt.verify_Ed25519_EdDSA(key.getEncoded());
        } catch (JOSEException e) {
            throw new RuntimeException(e); // TODO
        }
    }
}
