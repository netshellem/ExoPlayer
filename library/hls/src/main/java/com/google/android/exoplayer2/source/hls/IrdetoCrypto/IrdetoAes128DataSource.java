package com.google.android.exoplayer2.source.hls.IrdetoCrypto;

import android.net.Uri;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSourceInputStream;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.util.Assertions;

import java.io.File;

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
 * Created by Syliu on 19-01-2018.
 */

public class IrdetoAes128DataSource implements DataSource {
    private final DataSource upstream;
    private final byte[] iKeyData;
    private final byte[] encryptionIv;
    static {
        System.loadLibrary("native-lib");
    }
    private IrdetoCipherInputStream irdetoCipherInputStream;

    /**
     * @param upstream     The upstream {@link DataSource}.
     * @param iKeyData
     * @param encryptionIv The encryption initialization vector.
     */
    public IrdetoAes128DataSource(DataSource upstream, byte[] iKeyData,byte[] encryptionIv) {
        this.upstream = upstream;
        this.iKeyData = iKeyData;
        this.encryptionIv = encryptionIv;
    }

    @Override
    public long open(DataSpec dataSpec) throws IOException {
        //todo: call native method to wrap up secure store
        irdetoCipherInputStream = new IrdetoCipherInputStream(
                new DataSourceInputStream(upstream, dataSpec), iKeyData,encryptionIv);

        return C.LENGTH_UNSET;
    }

    @Override
    public void close() throws IOException {
        irdetoCipherInputStream = null;
        upstream.close();
    }

    @Override
    public int read(byte[] buffer, int offset, int readLength) throws IOException {
        Assertions.checkState(irdetoCipherInputStream != null);
        int bytesRead = irdetoCipherInputStream.read(buffer, offset, readLength);
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
