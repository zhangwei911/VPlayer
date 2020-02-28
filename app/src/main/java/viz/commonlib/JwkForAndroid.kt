package viz.commonlib

import com.auth0.jwk.InvalidPublicKeyException
import com.google.gson.JsonObject
import java.math.BigInteger
import java.security.KeyFactory
import java.security.NoSuchAlgorithmException
import java.security.PublicKey
import java.security.spec.InvalidKeySpecException
import java.security.spec.RSAPublicKeySpec
import android.util.Base64

/**
 * Represents a JSON Web Key (JWK) used to verify the signature of JWTs
 */
class JwkForAndroid
/**
 * Creates a new Jwk
 *
 * @param id                    kid
 * @param type                  kyt
 * @param algorithm             alg
 * @param usage                 use
 * @param operations            key_ops
 * @param certificateUrl        x5u
 * @param certificateChain      x5c
 * @param certificateThumbprint x5t
 * @param additionalAttributes  additional attributes not part of the standard ones
 */(
    val id: String,
    val type: String,
    val algorithm: String,
    val usage: String,
    val operationsAsList: List<String>,
    val certificateUrl: String?,
    val certificateChain: List<String>?,
    val certificateThumbprint: String?,
    val additionalAttributes: Map<String, Any>
) {

    /**
     * Creates a new Jwk
     *
     * @param id
     * @param type
     * @param algorithm
     * @param usage
     * @param operations
     * @param certificateUrl
     * @param certificateChain
     * @param certificateThumbprint
     * @param additionalAttributes
     */
    @Deprecated("The specification states that the 'key_ops' (operations) parameter contains an array value.\n" + "      Use {@link #Jwk(String, String, String, String, List, String, List, String, Map)}")
    constructor(
        id: String,
        type: String,
        algorithm: String,
        usage: String,
        operations: String,
        certificateUrl: String?,
        certificateChain: List<String>?,
        certificateThumbprint: String?,
        additionalAttributes: Map<String, Any>
    ) : this(
        id,
        type,
        algorithm,
        usage,
        listOf<String>(operations),
        certificateUrl,
        certificateChain,
        certificateThumbprint,
        additionalAttributes
    ) {
    }

    fun getOperations(): String? {
        if (operationsAsList == null || operationsAsList.isEmpty()) {
            return null
        }
        val sb = StringBuilder()
        val delimiter = ","
        for (operation in operationsAsList) {
            sb.append(operation)
            sb.append(delimiter)
        }
        val ops = sb.toString()
        return ops.substring(0, ops.length - delimiter.length)
    }

    /**
     * Returns a [PublicKey] if the `'alg'` is `'RSA'`
     *
     * @return a public key
     * @throws InvalidPublicKeyException if the key cannot be built or the key type is not RSA
     */
    @get:Throws(InvalidPublicKeyException::class)
    val publicKey: PublicKey
        get() {
            if (!PUBLIC_KEY_ALGORITHM.equals(type, ignoreCase = true)) {
                throw InvalidPublicKeyException("The key is not of type RSA", null)
            }
            return try {
                val kf =
                    KeyFactory.getInstance(PUBLIC_KEY_ALGORITHM)
                val modulus = BigInteger(
                    1,
                    Base64.decode(stringValue("n"), Base64.URL_SAFE)
                )
                val exponent = BigInteger(
                    1,
                    Base64.decode(stringValue("e"), Base64.URL_SAFE)
                )
                kf.generatePublic(RSAPublicKeySpec(modulus, exponent))
            } catch (e: InvalidKeySpecException) {
                throw InvalidPublicKeyException("Invalid public key", e)
            } catch (e: NoSuchAlgorithmException) {
                throw InvalidPublicKeyException("Invalid algorithm to generate key", e)
            }
        }

    private fun stringValue(key: String): String? {
        return additionalAttributes[key] as String?
    }

    override fun toString(): String {
        val jsonObject = JsonObject().apply {
            addProperty("kid", id)
            addProperty("kyt", type)
            addProperty("alg", algorithm)
            addProperty("use", usage)
            val jsonObjectInner = JsonObject()
            for ((key, value) in additionalAttributes) {
                jsonObjectInner.addProperty(key, value.toString())
            }
            add("extras", jsonObjectInner)
        }
        return jsonObject.toString()
    }

    companion object {
        private const val PUBLIC_KEY_ALGORITHM = "RSA"
        fun fromValues(map: Map<String, Any>): JwkForAndroid {
            val values: MutableMap<String, Any> = HashMap(map).toMutableMap()
            val kid = values.remove("kid") as String
            val kty = values.remove("kty") as String
            val alg = values.remove("alg") as String
            val use = values.remove("use") as String
            val keyOps = values.remove("key_ops")
            val x5u = values.remove("x5u") as String
            val x5c =
                values.remove("x5c") as List<String>
            val x5t = values.remove("x5t") as String
            requireNotNull(kty) { "Attributes $map are not from a valid jwk" }
            return if (keyOps is String) {
                JwkForAndroid(
                    kid,
                    kty,
                    alg,
                    use,
                    keyOps,
                    x5u,
                    x5c,
                    x5t,
                    values
                )
            } else {
                JwkForAndroid(
                    kid,
                    kty,
                    alg,
                    use,
                    keyOps as List<String>,
                    x5u,
                    x5c,
                    x5t,
                    values
                )
            }
        }
    }

}