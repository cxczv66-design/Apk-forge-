package com.example.engine

import org.bouncycastle.crypto.digests.Blake2bDigest
import org.bouncycastle.crypto.macs.Poly1305
import org.bouncycastle.crypto.params.KeyParameter
import org.bouncycastle.math.ec.rfc7748.X25519
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.SecureRandom
import java.security.MessageDigest

/**
 * تطبيق متوافق مع Libsodium `crypto_box_seal` (X25519 + BLAKE2b-192 + HSalsa20 + XSalsa20-Poly1305)
 * بالاعتماد على BouncyCastle المدمجة دون أي تبعيات خارجية، لتشفير أسرار GitHub Actions محلياً قبل إرسالها.
 */
object LibsodiumSealedBox {

    private val SIGMA = intArrayOf(0x61707865, 0x3320646e, 0x79622d32, 0x6b206574)
    private val ZERO_16 = ByteArray(16)

    fun seal(message: ByteArray, recipientPublicKey32: ByteArray, random: SecureRandom = SecureRandom()): ByteArray {
        require(recipientPublicKey32.size == 32) { "X25519 public key must be 32 bytes" }

        val ephemeralPriv = ByteArray(32)
        val ephemeralPub = ByteArray(32)
        X25519.generatePrivateKey(random, ephemeralPriv)
        X25519.generatePublicKey(ephemeralPriv, 0, ephemeralPub, 0)

        return try {
            val sharedRaw = ByteArray(32)
            X25519.scalarMult(ephemeralPriv, 0, recipientPublicKey32, 0, sharedRaw, 0)
            require(!sharedRaw.all { it == 0.toByte() }) { "Invalid X25519 shared point" }

            val boxKey = hsalsa20(sharedRaw, ZERO_16)
            sharedRaw.fill(0)

            val nonce = sealedBoxNonce(ephemeralPub, recipientPublicKey32)
            val boxed = secretBoxEasy(message, nonce, boxKey)
            boxKey.fill(0)

            val out = ByteArray(32 + boxed.size)
            System.arraycopy(ephemeralPub, 0, out, 0, 32)
            System.arraycopy(boxed, 0, out, 32, boxed.size)
            out
        } finally {
            ephemeralPriv.fill(0)
        }
    }

    fun open(
        sealedBox: ByteArray,
        recipientPublicKey32: ByteArray,
        recipientPrivateKey32: ByteArray
    ): ByteArray {
        require(recipientPublicKey32.size == 32) { "X25519 public key must be 32 bytes" }
        require(recipientPrivateKey32.size == 32) { "X25519 private key must be 32 bytes" }
        require(sealedBox.size >= 48) { "Sealed box too short (${sealedBox.size} bytes)" }

        val ephemeralPub = sealedBox.copyOfRange(0, 32)
        val boxed = sealedBox.copyOfRange(32, sealedBox.size)

        val sharedRaw = ByteArray(32)
        X25519.scalarMult(recipientPrivateKey32, 0, ephemeralPub, 0, sharedRaw, 0)
        require(!sharedRaw.all { it == 0.toByte() }) { "Invalid X25519 shared point" }

        val boxKey = hsalsa20(sharedRaw, ZERO_16)
        sharedRaw.fill(0)

        val nonce = sealedBoxNonce(ephemeralPub, recipientPublicKey32)
        val plain = secretBoxOpenEasy(boxed, nonce, boxKey)
        boxKey.fill(0)
        return plain
    }

    internal fun sealedBoxNonce(ephemeralPub32: ByteArray, recipientPub32: ByteArray): ByteArray {
        val digest = Blake2bDigest(192) // 24 bytes
        digest.update(ephemeralPub32, 0, 32)
        digest.update(recipientPub32, 0, 32)
        val nonce = ByteArray(24)
        digest.doFinal(nonce, 0)
        return nonce
    }

    /**
     * NaCl crypto_secretbox_xsalsa20poly1305: returns 16-byte Poly1305 MAC || ciphertext.
     */
    internal fun secretBoxEasy(message: ByteArray, nonce24: ByteArray, key32: ByteArray): ByteArray {
        require(nonce24.size == 24) { "XSalsa20 nonce must be 24 bytes" }
        require(key32.size == 32) { "XSalsa20 key must be 32 bytes" }

        val subKey = hsalsa20(key32, nonce24.copyOfRange(0, 16))
        val salsaNonce8 = nonce24.copyOfRange(16, 24)
        val keystream = salsa20Stream(subKey, salsaNonce8, 32 + message.size)
        subKey.fill(0)

        val polyKey = keystream.copyOfRange(0, 32)
        val ciphertext = ByteArray(message.size)
        for (i in message.indices) {
            ciphertext[i] = (message[i].toInt() xor keystream[32 + i].toInt()).toByte()
        }
        keystream.fill(0)

        val mac = computePoly1305(polyKey, ciphertext)
        polyKey.fill(0)

        val out = ByteArray(16 + ciphertext.size)
        System.arraycopy(mac, 0, out, 0, 16)
        System.arraycopy(ciphertext, 0, out, 16, ciphertext.size)
        return out
    }

    internal fun secretBoxOpenEasy(boxed: ByteArray, nonce24: ByteArray, key32: ByteArray): ByteArray {
        require(boxed.size >= 16) { "Ciphertext missing MAC" }
        require(nonce24.size == 24 && key32.size == 32)

        val mac = boxed.copyOfRange(0, 16)
        val ciphertext = boxed.copyOfRange(16, boxed.size)

        val subKey = hsalsa20(key32, nonce24.copyOfRange(0, 16))
        val salsaNonce8 = nonce24.copyOfRange(16, 24)
        val keystream = salsa20Stream(subKey, salsaNonce8, 32 + ciphertext.size)
        subKey.fill(0)

        val polyKey = keystream.copyOfRange(0, 32)
        val expectedMac = computePoly1305(polyKey, ciphertext)
        polyKey.fill(0)

        if (!MessageDigest.isEqual(mac, expectedMac)) {
            keystream.fill(0)
            throw SecurityException("SealedBox MAC verification failed")
        }

        val plain = ByteArray(ciphertext.size)
        for (i in ciphertext.indices) {
            plain[i] = (ciphertext[i].toInt() xor keystream[32 + i].toInt()).toByte()
        }
        keystream.fill(0)
        return plain
    }

    private fun computePoly1305(key32: ByteArray, data: ByteArray): ByteArray {
        val poly = Poly1305()
        poly.init(KeyParameter(key32))
        if (data.isNotEmpty()) {
            poly.update(data, 0, data.size)
        }
        val mac = ByteArray(16)
        poly.doFinal(mac, 0)
        return mac
    }

    internal fun hsalsa20(key32: ByteArray, in16: ByteArray): ByteArray {
        val x = IntArray(16)
        x[0] = SIGMA[0]
        x[1] = leInt(key32, 0)
        x[2] = leInt(key32, 4)
        x[3] = leInt(key32, 8)
        x[4] = leInt(key32, 12)
        x[5] = SIGMA[1]
        x[6] = leInt(in16, 0)
        x[7] = leInt(in16, 4)
        x[8] = leInt(in16, 8)
        x[9] = leInt(in16, 12)
        x[10] = SIGMA[2]
        x[11] = leInt(key32, 16)
        x[12] = leInt(key32, 20)
        x[13] = leInt(key32, 24)
        x[14] = leInt(key32, 28)
        x[15] = SIGMA[3]

        repeat(10) {
            // Column rounds
            x[4] = x[4] xor rotl(x[0] + x[12], 7)
            x[8] = x[8] xor rotl(x[4] + x[0], 9)
            x[12] = x[12] xor rotl(x[8] + x[4], 13)
            x[0] = x[0] xor rotl(x[12] + x[8], 18)

            x[9] = x[9] xor rotl(x[5] + x[1], 7)
            x[13] = x[13] xor rotl(x[9] + x[5], 9)
            x[1] = x[1] xor rotl(x[13] + x[9], 13)
            x[5] = x[5] xor rotl(x[1] + x[13], 18)

            x[14] = x[14] xor rotl(x[10] + x[6], 7)
            x[2] = x[2] xor rotl(x[14] + x[10], 9)
            x[6] = x[6] xor rotl(x[2] + x[14], 13)
            x[10] = x[10] xor rotl(x[6] + x[2], 18)

            x[3] = x[3] xor rotl(x[15] + x[11], 7)
            x[7] = x[7] xor rotl(x[3] + x[15], 9)
            x[11] = x[11] xor rotl(x[7] + x[3], 13)
            x[15] = x[15] xor rotl(x[11] + x[7], 18)

            // Row rounds
            x[1] = x[1] xor rotl(x[0] + x[3], 7)
            x[2] = x[2] xor rotl(x[1] + x[0], 9)
            x[3] = x[3] xor rotl(x[2] + x[1], 13)
            x[0] = x[0] xor rotl(x[3] + x[2], 18)

            x[6] = x[6] xor rotl(x[5] + x[4], 7)
            x[7] = x[7] xor rotl(x[6] + x[5], 9)
            x[4] = x[4] xor rotl(x[7] + x[6], 13)
            x[5] = x[5] xor rotl(x[4] + x[7], 18)

            x[11] = x[11] xor rotl(x[10] + x[9], 7)
            x[8] = x[8] xor rotl(x[11] + x[10], 9)
            x[9] = x[9] xor rotl(x[8] + x[11], 13)
            x[10] = x[10] xor rotl(x[9] + x[8], 18)

            x[12] = x[12] xor rotl(x[15] + x[14], 7)
            x[13] = x[13] xor rotl(x[12] + x[15], 9)
            x[14] = x[14] xor rotl(x[13] + x[12], 13)
            x[15] = x[15] xor rotl(x[14] + x[13], 18)
        }

        val out = ByteBuffer.allocate(32).order(ByteOrder.LITTLE_ENDIAN)
        out.putInt(x[0])
        out.putInt(x[5])
        out.putInt(x[10])
        out.putInt(x[15])
        out.putInt(x[6])
        out.putInt(x[7])
        out.putInt(x[8])
        out.putInt(x[9])
        return out.array()
    }

    private fun salsa20Stream(key32: ByteArray, nonce8: ByteArray, length: Int): ByteArray {
        val out = ByteArray(length)
        var offset = 0
        var counter = 0L
        while (offset < length) {
            val block = salsa20Block(key32, nonce8, counter)
            val copyLen = minOf(64, length - offset)
            System.arraycopy(block, 0, out, offset, copyLen)
            offset += copyLen
            counter++
        }
        return out
    }

    private fun salsa20Block(key32: ByteArray, nonce8: ByteArray, counter: Long): ByteArray {
        val init = IntArray(16)
        init[0] = SIGMA[0]
        init[1] = leInt(key32, 0)
        init[2] = leInt(key32, 4)
        init[3] = leInt(key32, 8)
        init[4] = leInt(key32, 12)
        init[5] = SIGMA[1]
        init[6] = leInt(nonce8, 0)
        init[7] = leInt(nonce8, 4)
        init[8] = (counter and 0xFFFFFFFFL).toInt()
        init[9] = ((counter ushr 32) and 0xFFFFFFFFL).toInt()
        init[10] = SIGMA[2]
        init[11] = leInt(key32, 16)
        init[12] = leInt(key32, 20)
        init[13] = leInt(key32, 24)
        init[14] = leInt(key32, 28)
        init[15] = SIGMA[3]

        val x = init.copyOf()
        repeat(10) {
            x[4] = x[4] xor rotl(x[0] + x[12], 7)
            x[8] = x[8] xor rotl(x[4] + x[0], 9)
            x[12] = x[12] xor rotl(x[8] + x[4], 13)
            x[0] = x[0] xor rotl(x[12] + x[8], 18)

            x[9] = x[9] xor rotl(x[5] + x[1], 7)
            x[13] = x[13] xor rotl(x[9] + x[5], 9)
            x[1] = x[1] xor rotl(x[13] + x[9], 13)
            x[5] = x[5] xor rotl(x[1] + x[13], 18)

            x[14] = x[14] xor rotl(x[10] + x[6], 7)
            x[2] = x[2] xor rotl(x[14] + x[10], 9)
            x[6] = x[6] xor rotl(x[2] + x[14], 13)
            x[10] = x[10] xor rotl(x[6] + x[2], 18)

            x[3] = x[3] xor rotl(x[15] + x[11], 7)
            x[7] = x[7] xor rotl(x[3] + x[15], 9)
            x[11] = x[11] xor rotl(x[7] + x[3], 13)
            x[15] = x[15] xor rotl(x[11] + x[7], 18)

            x[1] = x[1] xor rotl(x[0] + x[3], 7)
            x[2] = x[2] xor rotl(x[1] + x[0], 9)
            x[3] = x[3] xor rotl(x[2] + x[1], 13)
            x[0] = x[0] xor rotl(x[3] + x[2], 18)

            x[6] = x[6] xor rotl(x[5] + x[4], 7)
            x[7] = x[7] xor rotl(x[6] + x[5], 9)
            x[4] = x[4] xor rotl(x[7] + x[6], 13)
            x[5] = x[5] xor rotl(x[4] + x[7], 18)

            x[11] = x[11] xor rotl(x[10] + x[9], 7)
            x[8] = x[8] xor rotl(x[11] + x[10], 9)
            x[9] = x[9] xor rotl(x[8] + x[11], 13)
            x[10] = x[10] xor rotl(x[9] + x[8], 18)

            x[12] = x[12] xor rotl(x[15] + x[14], 7)
            x[13] = x[13] xor rotl(x[12] + x[15], 9)
            x[14] = x[14] xor rotl(x[13] + x[12], 13)
            x[15] = x[15] xor rotl(x[14] + x[13], 18)
        }

        val buf = ByteBuffer.allocate(64).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until 16) {
            buf.putInt(x[i] + init[i])
        }
        return buf.array()
    }

    private fun leInt(src: ByteArray, offset: Int): Int {
        return (src[offset].toInt() and 0xFF) or
            ((src[offset + 1].toInt() and 0xFF) shl 8) or
            ((src[offset + 2].toInt() and 0xFF) shl 16) or
            ((src[offset + 3].toInt() and 0xFF) shl 24)
    }

    private fun rotl(value: Int, shift: Int): Int = (value shl shift) or (value ushr (32 - shift))
}
