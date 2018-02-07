package com.irdeto.chinadrm.hls;


import android.net.Uri;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSourceInputStream;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.util.Assertions;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.spec.AlgorithmParameterSpec;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * A {@link DataSource} that decrypts data read from an upstream source, encrypted with AES-128 with
 * a 128-bit key and PKCS7 padding.
 * <p>
 * Note that this {@link DataSource} does not support being opened from arbitrary offsets. It is
 * designed specifically for reading whole files as defined in an HLS media playlist. For this
 * reason the implementation is private to the HLS package.
 */
/* package */ final class Aes128DataSource implements DataSource {

    private final DataSource upstream;
    private final byte[] encryptionKey;
    private final byte[] encryptionIv;

    private CipherInputStream cipherInputStream;

    /**
     * @param upstream The upstream {@link DataSource}.
     * @param encryptionKey The encryption key.
     * @param encryptionIv The encryption initialization vector.
     */
    public Aes128DataSource(DataSource upstream, byte[] encryptionKey, byte[] encryptionIv) {
        this.upstream = upstream;
        this.encryptionKey = encryptionKey;
        this.encryptionIv = encryptionIv;
    }

    @Override
    public long open(DataSpec dataSpec) throws IOException {
        Cipher cipher;
        try {
            cipher = Cipher.getInstance("AES/CBC/PKCS7Padding");
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new RuntimeException(e);
        }

        Key cipherKey = new SecretKeySpec(encryptionKey, "AES");
        AlgorithmParameterSpec cipherIV = new IvParameterSpec(encryptionIv);

        try {
            cipher.init(Cipher.DECRYPT_MODE, cipherKey, cipherIV);
        } catch (InvalidKeyException | InvalidAlgorithmParameterException e) {
            throw new RuntimeException(e);
        }

        cipherInputStream = new CipherInputStream(
                new DataSourceInputStream(upstream, dataSpec), cipher);

        return C.LENGTH_UNSET;
    }

    @Override
    public void close() throws IOException {
        cipherInputStream = null;
        upstream.close();
    }

    @Override
    public int read(byte[] buffer, int offset, int readLength) throws IOException {
        Assertions.checkState(cipherInputStream != null);
        int bytesRead = cipherInputStream.read(buffer, offset, readLength);
        if (bytesRead < 0) {
            return C.RESULT_END_OF_INPUT;
        }
        return bytesRead;
    }

    @Override
    public Uri getUri() {
        return upstream.getUri();
    }

}
