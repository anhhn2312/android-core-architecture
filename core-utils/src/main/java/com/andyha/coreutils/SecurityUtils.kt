package com.andyha.coreutils

import android.security.keystore.KeyProperties
import android.util.Base64
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo
import org.bouncycastle.cert.X509CertificateHolder
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.openssl.PEMException
import org.bouncycastle.openssl.PEMKeyPair
import org.bouncycastle.openssl.PEMParser
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter
import org.bouncycastle.openssl.jcajce.JcaPEMWriter
import org.bouncycastle.operator.ContentSigner
import org.bouncycastle.operator.OperatorCreationException
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.bouncycastle.pkcs.PKCS10CertificationRequest
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder
import timber.log.Timber
import java.io.IOException
import java.io.StringReader
import java.io.StringWriter
import java.math.BigInteger
import java.security.*
import java.security.cert.Certificate
import javax.crypto.*
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.security.auth.x500.X500Principal


object SecurityUtils {

    const val RSA_CIPHER_TRANSFORMATION = KeyProperties.KEY_ALGORITHM_RSA
    const val RSA_SIGNATURE_TRANSFORMATION = "SHA256withRSA"
    const val RSA_CSR_CONTENT_SIGNER = "SHA256withRSA"
    const val AES_KEY_SIZE = 256
    const val AES_CIPHER_TRANSFORMATION =
        "${KeyProperties.KEY_ALGORITHM_AES}/${KeyProperties.BLOCK_MODE_CBC}/${KeyProperties.ENCRYPTION_PADDING_PKCS7}"

    fun encodeBase64(rawData: ByteArray) = Base64.encodeToString(rawData, Base64.DEFAULT)
    fun encodeBase64NoWrap(rawData: ByteArray) = Base64.encodeToString(rawData, Base64.NO_WRAP)

    @Throws(IllegalArgumentException::class)
    fun decodeBase64(encodedData: String) = Base64.decode(encodedData, Base64.DEFAULT)

    @Throws(IllegalArgumentException::class)
    fun decodeBase64NoWrap(encodedData: String) = Base64.decode(encodedData, Base64.NO_WRAP)

    @Throws(IOException::class)
    fun convertPublicKeyToString(publicKey: PublicKey): String {
        val sw = StringWriter()
        val pemWriter = JcaPEMWriter(sw)
        pemWriter.writeObject(publicKey)
        pemWriter.close()
        return sw.toString()
    }

    @Throws(IOException::class)
    fun convertPrivateKeyToString(privateKey: PrivateKey): String {
        val sw = StringWriter()
        val pemWriter = JcaPEMWriter(sw)
        pemWriter.writeObject(privateKey)
        pemWriter.close()
        return sw.toString()
    }

    @Throws(IOException::class)
    fun convertCertificateToString(certificate: Certificate): String {
        val sw = StringWriter()
        val pemWriter = JcaPEMWriter(sw)
        pemWriter.writeObject(certificate)
        pemWriter.close()
        return sw.toString().trimIndent()
    }

    @Throws(
        IOException::class,
        ClassCastException::class,
        PEMException::class,
        IllegalArgumentException::class
    )
    fun convertPublicKeyFromString(publicKeyPEM: String): PublicKey {
        val pemParser = PEMParser(StringReader(publicKeyPEM))
        val pemPublicKey = pemParser.readObject() as SubjectPublicKeyInfo
        val converter = JcaPEMKeyConverter()
        return converter.getPublicKey(SubjectPublicKeyInfo.getInstance(pemPublicKey))
    }

    @Throws(
        IOException::class,
        ClassCastException::class,
        PEMException::class,
        IllegalArgumentException::class
    )
    fun convertKeyPairFromString(privateKeyPEM: String): KeyPair {
        val pemParser = PEMParser(StringReader(privateKeyPEM))
        val pemKeyPair = pemParser.readObject() as PEMKeyPair
        val converter = JcaPEMKeyConverter()
        return converter.getKeyPair(pemKeyPair)
    }

    @Throws(
        IOException::class,
        ClassCastException::class,
        PEMException::class,
        IllegalArgumentException::class
    )
    fun convertPrivateKeyFromString(privateKeyPEM: String): PrivateKey {
        val keyPair = convertKeyPairFromString(privateKeyPEM)
        return keyPair.private
    }

    @Throws(
        IOException::class,
        ClassCastException::class,
        PEMException::class,
        IllegalArgumentException::class
    )
    fun convertCertificateFromString(certificatePEM: String): java.security.cert.X509Certificate {
        val pemParser = PEMParser(StringReader(certificatePEM.trimIndent()))
        val certHolder = pemParser.readObject() as X509CertificateHolder
        val converter = JcaX509CertificateConverter()
        return converter.getCertificate(certHolder)
    }

    @Throws(
        NoSuchAlgorithmException::class,
        InvalidParameterException::class
    )
    fun generateRsaKey(keySize: Int): KeyPair {
        val keyPairGenerator: KeyPairGenerator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA)
        keyPairGenerator.initialize(keySize)
        val pair = keyPairGenerator.generateKeyPair()

        try {
            Timber.d(
                listOf(
                    "SecurityUtils:",
                    "PublicKeyNoWrap:",
                    encodeBase64NoWrap(pair.public.encoded),
                    "PublicKeyPEM:",
                    convertPublicKeyToString(pair.public),
                    "PrivateKeyNoWrap:",
                    encodeBase64NoWrap(pair.private.encoded),
                    "PrivateKeyPEM:",
                    convertPrivateKeyToString(pair.private),
                ).joinToString("\n")
            )
        } catch (e: Exception) {
        }

        return pair
    }

    @Throws(
        NoSuchAlgorithmException::class,
        NoSuchPaddingException::class,
        InvalidKeyException::class,
        UnsupportedOperationException::class,
        IllegalStateException::class,
        IllegalBlockSizeException::class,
        BadPaddingException::class,
        AEADBadTagException::class
    )
    fun encryptRsa(publicKey: PublicKey, rawData: ByteArray): String {
        val cipher = Cipher.getInstance(RSA_CIPHER_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, publicKey)
        val encryptedBytes = cipher.doFinal(rawData)
        return encodeBase64NoWrap(encryptedBytes)
    }

    @Throws(
        NoSuchAlgorithmException::class,
        NoSuchPaddingException::class,
        InvalidKeyException::class,
        UnsupportedOperationException::class,
        IllegalStateException::class,
        IllegalBlockSizeException::class,
        BadPaddingException::class,
        AEADBadTagException::class,
        IllegalArgumentException::class
    )
    fun decryptRsa(privateKey: PrivateKey, encryptedData: String): ByteArray {
        val cipher = Cipher.getInstance(RSA_CIPHER_TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, privateKey)
        return cipher.doFinal(decodeBase64NoWrap(encryptedData))
    }

    @Throws(
        NoSuchAlgorithmException::class,
        InvalidKeyException::class,
        SignatureException::class
    )
    fun signRsaSignature(privateKey: PrivateKey, vararg dataset: ByteArray): String {
        val sig = Signature.getInstance(RSA_SIGNATURE_TRANSFORMATION)
        sig.initSign(privateKey)
        for (data in dataset) {
            sig.update(data)
        }
        val signatureBytes = sig.sign()
        Timber.d("Signed bytes: ${signatureBytes.toHex()}")
        return encodeBase64NoWrap(signatureBytes)
    }

    @Throws(
        NoSuchAlgorithmException::class,
        InvalidKeyException::class,
        SignatureException::class,
        IllegalArgumentException::class
    )
    fun verifyRsaSignature(publicKey: PublicKey, signature: String, vararg dataset: ByteArray): Boolean {
        val sig = Signature.getInstance(RSA_SIGNATURE_TRANSFORMATION)
        sig.initVerify(publicKey)
        for (data in dataset) {
            sig.update(data)
        }
        return sig.verify(decodeBase64NoWrap(signature))
    }

    @Throws(
        IllegalArgumentException::class,
        OperatorCreationException::class,
        IllegalStateException::class,
        NullPointerException::class,
        IOException::class,
        ClassCastException::class,
        PEMException::class,
    )
    fun generateRsaCSR(keyPair: KeyPair, subject: X500Principal): String {
        val p10Builder: PKCS10CertificationRequestBuilder = JcaPKCS10CertificationRequestBuilder(subject, keyPair.public)
        val csBuilder = JcaContentSignerBuilder(RSA_CSR_CONTENT_SIGNER)
        val signer: ContentSigner = csBuilder.build(keyPair.private)
        val csr: PKCS10CertificationRequest = p10Builder.build(signer)

        // write to str
        val writer = StringWriter()
        val pemWriter = JcaPEMWriter(writer)
        pemWriter.writeObject(csr)
        pemWriter.flush()
        pemWriter.close()
        return writer.toString()
    }

    @Throws(InvalidKeyException::class)
    fun generateAesKey(keySize: Int = AES_KEY_SIZE): String {
        if (keySize % Byte.SIZE_BITS != 0) {
            throw InvalidKeyException("Incorrect length for key!")
        }
        val length = keySize / Byte.SIZE_BITS
        return randomString(length)
    }

    fun randomString(size: Int): String {
        val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
        return (1..size)
            .map { allowedChars.random() }
            .joinToString("")
    }

    fun ByteArray.toHex(): String = joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }

    @Throws(
        IllegalArgumentException::class,
        NullPointerException::class,
        NoSuchAlgorithmException::class,
        NoSuchPaddingException::class,
        UnsupportedOperationException::class,
        InvalidKeyException::class,
        InvalidAlgorithmParameterException::class,
        IllegalStateException::class,
        IllegalBlockSizeException::class,
        BadPaddingException::class,
        AEADBadTagException::class
    )
    fun encryptAes(password: ByteArray, rawData: ByteArray, iv: ByteArray): String {
        val secret = SecretKeySpec(password, AES_CIPHER_TRANSFORMATION)
        val ivSpec = IvParameterSpec(iv)
        val cipher = Cipher.getInstance(AES_CIPHER_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secret, ivSpec)
        return encodeBase64NoWrap(cipher.doFinal(rawData))
    }

    @Throws(
        IllegalArgumentException::class,
        NullPointerException::class,
        NoSuchAlgorithmException::class,
        NoSuchPaddingException::class,
        UnsupportedOperationException::class,
        InvalidKeyException::class,
        InvalidAlgorithmParameterException::class,
        IllegalStateException::class,
        IllegalBlockSizeException::class,
        BadPaddingException::class,
        AEADBadTagException::class
    )
    fun decryptAes(password: ByteArray, encryptedData: String, iv: ByteArray): ByteArray {
        val secret = SecretKeySpec(password, AES_CIPHER_TRANSFORMATION)
        val ivSpec = IvParameterSpec(iv)
        val cipher = Cipher.getInstance(AES_CIPHER_TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secret, ivSpec)
        return cipher.doFinal(decodeBase64NoWrap(encryptedData))
    }

    @Throws(
        NoSuchAlgorithmException::class,
        NullPointerException::class,
        NumberFormatException::class
    )
    fun getMD5(data: ByteArray): String {
        val md = MessageDigest.getInstance(KeyProperties.DIGEST_MD5)
        return BigInteger(1, md.digest(data)).toString(16).padStart(32, '0')
    }

    @Throws(
        NoSuchAlgorithmException::class,
        NullPointerException::class,
        NumberFormatException::class
    )
    fun getSHA256(data: ByteArray): ByteArray {
        val sha = MessageDigest.getInstance(KeyProperties.DIGEST_SHA256)
        return sha.digest(data)
//        return BigInteger(1, sha.digest(data)).toString(16).padStart(64, '0')
    }

    @Throws(
        NoSuchAlgorithmException::class,
        IllegalArgumentException::class,
        InvalidKeyException::class,
        IllegalStateException::class,
        NullPointerException::class,
        NumberFormatException::class
    )
    fun getHmacSHA256(password: ByteArray, vararg dataset: ByteArray): ByteArray {
        val algo = KeyProperties.KEY_ALGORITHM_HMAC_SHA256
        val hMacSha256: Mac = Mac.getInstance(algo)
        val secretKey = SecretKeySpec(password, algo)
        hMacSha256.init(secretKey)
        for (data in dataset) {
            hMacSha256.update(data)
        }

        return hMacSha256.doFinal()
    }

    fun hMacSha256ToString(hMacSha256: ByteArray): String {
        return BigInteger(1, hMacSha256).toString(16).padStart(64, '0')
    }

    fun example() {
        try {
            val data = "This is a sample string"

            val encoded = encodeBase64(data.toByteArray())
            Timber.d("encryptionManager: encoded: $encoded")
            val decoded = decodeBase64(encoded)
            Timber.d("encryptionManager: decoded: ${String(decoded)}")
            val _rsaPair = generateRsaKey(2048)
            val rsaPair = Pair(convertPublicKeyToString(_rsaPair.public), convertPrivateKeyToString(_rsaPair.private))
            Timber.d("encryptionManager:\npubKey: \n${rsaPair.first}\npriKey: \n${rsaPair.second}")
            val pub2 = convertPublicKeyFromString(rsaPair.first)
            val priv2 = convertPrivateKeyFromString(rsaPair.second)
            Timber.d("encryptionManager:\npub2: \n${convertPublicKeyToString(pub2)}\npri2: \n${convertPrivateKeyToString(priv2)}")

            val data1 = "1645441788293eyIzNDIxMyI6eyI2Ijp7IjIiOiIxIn19fQ=="
            val signature1 = "iTIMGWEMxVSrbfLzkF/K3eo+GjGdY3xxEIdtAj4CT9ILBzPMBe88ZOgGJqN/q8WVrmj7OnUqSrVU/EO85+hbRx5ovvzh0UVvWkxY5HrV4gNB/i0gl93d2iJBsPBKwcPZxwAAY546xdPIe8ZfINt4FRiP1o5NEP9+SVl4+bXGCU4B7n3t+Zo8ayOI+RjH6flk+Xhi591Ik5zVk1WmbN+rySTyIMV7ESfxg+t4n7K5cTK0u46v4xEuNsm8ZmCoz17sZJNzlCJ2BNEZNGHQozPQprc7IVOnerZpAqta0aEo3kV4zyRqpttsi1/voRTd1DZwdGTwfI/nrfDDVh0PmMxRyg=="

            val encrypted = encryptRsa(_rsaPair.public, data1.toByteArray())
            Timber.d("encryptionManager: encrypted: $encrypted")
            val decrypted = decryptRsa(_rsaPair.private, encrypted)
            Timber.d("encryptionManager: decrypted: ${String(decrypted)}")

            val sign = signRsaSignature(_rsaPair.private, data1.toByteArray())
            Timber.d("encryptionManager: sign: $sign")
            val verify = verifyRsaSignature(_rsaPair.public, sign, data1.toByteArray())
            val verify2 = verifyRsaSignature(_rsaPair.public, sign, "1645441788293".toByteArray(), "eyIzNDIxMyI6eyI2Ijp7IjIiOiIxIn19fQ==".toByteArray())
            Timber.d("encryptionManager: data1: ${encodeBase64NoWrap(data1.toByteArray())}")
            Timber.d("encryptionManager: verify1: $verify")
            Timber.d("encryptionManager: verify2: $verify2")

            val aesPass = generateAesKey()
            Timber.d("encryptionManager: aesPass: $aesPass")

            val iv = "0123456789123456"

            val aesEncrypted = encryptAes(aesPass.toByteArray(), data.toByteArray(), iv.toByteArray())
            Timber.d("encryptionManager: aesEncrypted: $aesEncrypted")
            val aesDecrypted = decryptAes(aesPass.toByteArray(), aesEncrypted, iv.toByteArray())
            Timber.d("encryptionManager: aesDecrypted: ${String(aesDecrypted)}")

            val md5 = getMD5(data.toByteArray())
            Timber.d("encryptionManager: md5: $md5")

            val csr = generateRsaCSR(
                _rsaPair,
                X500Principal("CN=Requested Test Certificate, OU=JavaSoft, O=Sun Microsystems, C=US")
            )
            Timber.d("encryptionManager: CSR: \n$csr")

            val hmac = getHmacSHA256("asdasd".toByteArray(), "123".toByteArray(), "456".toByteArray())
            Timber.d("encryptionManager: HMAC_MD5: \n${hMacSha256ToString(hmac)}")

            val certStr1 = """
                -----BEGIN CERTIFICATE-----
                MIIH/TCCBeWgAwIBAgIQaBYE3/M08XHYCnNVmcFBcjANBgkqhkiG9w0BAQsFADBy
                MQswCQYDVQQGEwJVUzEOMAwGA1UECAwFVGV4YXMxEDAOBgNVBAcMB0hvdXN0b24x
                ETAPBgNVBAoMCFNTTCBDb3JwMS4wLAYDVQQDDCVTU0wuY29tIEVWIFNTTCBJbnRl
                cm1lZGlhdGUgQ0EgUlNBIFIzMB4XDTIwMDQwMTAwNTgzM1oXDTIxMDcxNjAwNTgz
                M1owgb0xCzAJBgNVBAYTAlVTMQ4wDAYDVQQIDAVUZXhhczEQMA4GA1UEBwwHSG91
                c3RvbjERMA8GA1UECgwIU1NMIENvcnAxFjAUBgNVBAUTDU5WMjAwODE2MTQyNDMx
                FDASBgNVBAMMC3d3dy5zc2wuY29tMR0wGwYDVQQPDBRQcml2YXRlIE9yZ2FuaXph
                dGlvbjEXMBUGCysGAQQBgjc8AgECDAZOZXZhZGExEzARBgsrBgEEAYI3PAIBAxMC
                VVMwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQDHheRkbb1FCc7xRKst
                wK0JIGaKY8t7JbS2bQ2b6YIJDgnHuIYHqBrCUV79oelikkokRkFvcvpaKinFHDQH
                UpWEI6RUERYmSCg3O8Wi42uOcV2B5ZabmXCkwdxY5Ecl51BbM8UnGdoAGbdNmiRm
                SmTjcs+lhMxg4fFY6lBpiEVFiGUjGRR+61R67Lz6U4KJeLNcCm07QwFYKBmpi08g
                dygSvRdUw55Jopredj+VGtjUkB4hFT4GQX/ght69Rlqz/+8u0dEQkhuUuucrqalm
                SGy43HRwBfDKFwYeWM7CPMd5e/dO+t08t8PbjzVTTv5hQDCsEYIV2T7AFI9ScNxM
                kh7/AgMBAAGjggNBMIIDPTAfBgNVHSMEGDAWgBS/wVqH/yj6QT39t0/kHa+gYVgp
                vTB/BggrBgEFBQcBAQRzMHEwTQYIKwYBBQUHMAKGQWh0dHA6Ly93d3cuc3NsLmNv
                bS9yZXBvc2l0b3J5L1NTTGNvbS1TdWJDQS1FVi1TU0wtUlNBLTQwOTYtUjMuY3J0
                MCAGCCsGAQUFBzABhhRodHRwOi8vb2NzcHMuc3NsLmNvbTAfBgNVHREEGDAWggt3
                d3cuc3NsLmNvbYIHc3NsLmNvbTBfBgNVHSAEWDBWMAcGBWeBDAEBMA0GCyqEaAGG
                9ncCBQEBMDwGDCsGAQQBgqkwAQMBBDAsMCoGCCsGAQUFBwIBFh5odHRwczovL3d3
                dy5zc2wuY29tL3JlcG9zaXRvcnkwHQYDVR0lBBYwFAYIKwYBBQUHAwIGCCsGAQUF
                BwMBMEgGA1UdHwRBMD8wPaA7oDmGN2h0dHA6Ly9jcmxzLnNzbC5jb20vU1NMY29t
                LVN1YkNBLUVWLVNTTC1SU0EtNDA5Ni1SMy5jcmwwHQYDVR0OBBYEFADAFUIazw5r
                ZIHapnRxIUnpw+GLMA4GA1UdDwEB/wQEAwIFoDCCAX0GCisGAQQB1nkCBAIEggFt
                BIIBaQFnAHcA9lyUL9F3MCIUVBgIMJRWjuNNExkzv98MLyALzE7xZOMAAAFxM0ho
                bwAABAMASDBGAiEA6xeliNR8Gk/63pYdnS/vOx/CjptEMEv89WWh1/urWIECIQDy
                BreHU25DzwukQaRQjwW655ZLkqCnxbxQWRiOemj9JAB1AJQgvB6O1Y1siHMfgosi
                LA3R2k1ebE+UPWHbTi9YTaLCAAABcTNIaNwAAAQDAEYwRAIgGRE4wzabNRdD8kq/
                vFP3tQe2hm0x5nXulowh4Ibw3lkCIFYb/3lSDplS7AcR4r+XpWtEKSTFWJmNCRbc
                XJur2RGBAHUA7sCV7o1yZA+S48O5G8cSo2lqCXtLahoUOOZHssvtxfkAAAFxM0ho
                8wAABAMARjBEAiB6IvboWss3R4ItVwjebl7D3yoFaX0NDh2dWhhgwCxrHwIgCfq7
                ocMC5t+1ji5M5xaLmPC4I+WX3I/ARkWSyiO7IQcwDQYJKoZIhvcNAQELBQADggIB
                ACeuur4QnujqmguSrHU3mhf+cJodzTQNqo4tde+PD1/eFdYAELu8xF+0At7xJiPY
                i5RKwilyP56v+3iY2T9lw7S8TJ041VLhaIKp14MzSUzRyeoOAsJ7QADMClHKUDlH
                UU2pNuo88Y6igovT3bsnwJNiEQNqymSSYhktw0taduoqjqXn06gsVioWTVDXysd5
                qEx4t6sIgIcMm26YH1vJpCQEhKpc2y07gRkklBZRtMjThv4cXyyMX7uTcdT7AJBP
                ueifCoV25JxXuo8d5139gwP1BAe7IBVPx2u7KN/UyOXdZmwMf/TmFGwDdCfsyHf/
                ZsB2wLHozTYoAVmQ9FoU1JLgcVivqJ+vNlBhHXhlxMdN0j80R9Nz6EIglQjeK3O8
                I/cFGm/B8+42hOlCId9ZdtndJcRJVji0wD0qwevCafA9jJlHv/jsE+I9Uz6cpCyh
                sw+lrFdxUgqU58axqeK89FR+No4q0IIO+Ji1rJKr9nkSB0BqXozVnE1YB/KLvdIs
                uYZJuqb2pKku+zzT6gUwHUTZvBiNOtXL4Nxwc/KT7WzOSd2wP10QI8DKg4vfiNDs
                HWmB1c4Kji6gOgA5uSUzaGmq/v4VncK5Ur+n9LbfnfLc28J5ft/GotinMyDk3iar
                F10YlqcOmeX1uFmKbdi/XorGlkCoMF3TDx8rmp9DBiB/
                -----END CERTIFICATE-----
            """.trimIndent()

            val cert = convertCertificateFromString(certStr1)
            val certStr2 = convertCertificateToString(cert)

            Timber.d("encryptionManager: certStr: \n$certStr2,\nisSame = ${certStr1 == certStr2}")

        } catch (e: Exception) {
            Timber.e("encryptionManager: $e")
        }
    }
}