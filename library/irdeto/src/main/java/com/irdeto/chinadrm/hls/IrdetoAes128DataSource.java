package com.irdeto.chinadrm.hls;

import android.net.Uri;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSourceInputStream;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.util.Assertions;
import com.irdeto.drm.ChinaDrm;

import java.io.IOException;

public class IrdetoAes128DataSource implements DataSource {
    public ChinaDrm drm;
    private final DataSource upstream;
    private IrdetoCipherInputStream cipherInputStream;
    private String line;

    public IrdetoAes128DataSource(DataSource upstream, ChinaDrm drm, String line)
    {
        this.drm = drm;
        this.upstream = upstream;
        this.line = line;
    }

    @Override
    public long open(DataSpec dataSpec) throws IOException {
        cipherInputStream = new IrdetoCipherInputStream(
                new DataSourceInputStream(upstream, dataSpec),  drm, line);
        return C.LENGTH_UNSET;
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

    @Override
    public void close() throws IOException {
        drm.destorySession();
    }
}
