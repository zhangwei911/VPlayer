package viz.commonlib

import com.auth0.jwt.exceptions.JWTDecodeException
import com.auth0.jwt.impl.JWTParser
import com.auth0.jwt.interfaces.Claim
import com.auth0.jwt.interfaces.DecodedJWT
import com.auth0.jwt.interfaces.Header
import com.auth0.jwt.interfaces.Payload
import java.util.Date
import android.util.Base64
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

class JWtDecoderForAndroid :
    DecodedJWT {
    private var parts: Array<String>
    private var header: Header
    private var payload: Payload

    constructor(converter: JWTParser, jwt: String) {
        parts = splitToken(jwt)
        val headerJson: String
        val payloadJson: String
        try {
            headerJson =
                Base64.decode(parts[0].toByteArray(Charset.forName("utf-8")), Base64.URL_SAFE)
                    .toString(
                        StandardCharsets.UTF_8
                    )
            payloadJson =
                Base64.decode(parts[1].toByteArray(Charset.forName("utf-8")), Base64.URL_SAFE)
                    .toString(
                        StandardCharsets.UTF_8
                    )
        } catch (e: NullPointerException) {
            throw JWTDecodeException("The UTF-8 Charset isn't initialized.", e)
        }
        header = converter.parseHeader(headerJson)
        payload = converter.parsePayload(payloadJson)
    }

    constructor(jwt: String) : this(JWTParser(), jwt)

    override fun getAlgorithm(): String {
        return header.algorithm
    }

    override fun getType(): String {
        return header.type
    }

    override fun getContentType(): String {
        return header.contentType
    }

    override fun getKeyId(): String {
        return header.keyId
    }

    override fun getHeaderClaim(name: String): Claim {
        return header.getHeaderClaim(name)
    }

    override fun getIssuer(): String {
        return payload.issuer
    }

    override fun getSubject(): String {
        return payload.subject
    }

    override fun getAudience(): List<String> {
        return payload.audience
    }

    override fun getExpiresAt(): Date {
        return payload.expiresAt
    }

    override fun getNotBefore(): Date {
        return payload.notBefore
    }

    override fun getIssuedAt(): Date {
        return payload.issuedAt
    }

    override fun getId(): String {
        return payload.id
    }

    override fun getClaim(name: String): Claim {
        return payload.getClaim(name)
    }

    override fun getClaims(): Map<String, Claim> {
        return payload.claims
    }

    override fun getHeader(): String {
        return parts[0]
    }

    override fun getPayload(): String {
        return parts[1]
    }

    override fun getSignature(): String {
        return parts[2]
    }

    override fun getToken(): String {
        return String.format("%s.%s.%s", parts[0], parts[1], parts[2])
    }

    @Throws(JWTDecodeException::class)
    fun splitToken(token: String): Array<String> {
        var parts: Array<String> = token.split("\\.".toRegex()).toTypedArray()
        if (parts.size == 2 && token.endsWith(".")) { //Tokens with alg='none' have empty String as Signature.
            parts = arrayOf(parts[0], parts[1], "")
        }
        if (parts.size != 3) {
            throw JWTDecodeException(
                String.format(
                    "The token was expected to have 3 parts, but got %s.",
                    parts.size
                )
            )
        }
        return parts
    }
}