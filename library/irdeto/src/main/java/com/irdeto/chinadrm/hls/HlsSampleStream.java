package com.irdeto.chinadrm.hls;

import com.google.android.exoplayer2.FormatHolder;
import com.google.android.exoplayer2.decoder.DecoderInputBuffer;
import com.google.android.exoplayer2.source.SampleStream;
import java.io.IOException;

/**
 * {@link SampleStream} for a particular track group in HLS.
 */
/* package */ final class HlsSampleStream implements SampleStream {

    public final int group;

    private final HlsSampleStreamWrapper sampleStreamWrapper;

    public HlsSampleStream(HlsSampleStreamWrapper sampleStreamWrapper, int group) {
        this.sampleStreamWrapper = sampleStreamWrapper;
        this.group = group;
    }

    @Override
    public boolean isReady() {
        return sampleStreamWrapper.isReady(group);
    }

    @Override
    public void maybeThrowError() throws IOException {
        sampleStreamWrapper.maybeThrowError();
    }

    @Override
    public int readData(FormatHolder formatHolder, DecoderInputBuffer buffer, boolean requireFormat) {
        return sampleStreamWrapper.readData(group, formatHolder, buffer, requireFormat);
    }

    @Override
    public void skipData(long positionUs) {
        sampleStreamWrapper.skipData(group, positionUs);
    }

}
