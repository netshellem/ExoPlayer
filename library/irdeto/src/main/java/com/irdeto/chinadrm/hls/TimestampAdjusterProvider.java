package com.irdeto.chinadrm.hls;

import android.util.SparseArray;
import com.google.android.exoplayer2.util.TimestampAdjuster;

/**
 * Provides {@link TimestampAdjuster} instances for use during HLS playbacks.
 */
public final class TimestampAdjusterProvider {

    // TODO: Prevent this array from growing indefinitely large by removing adjusters that are no
    // longer required.
    private final SparseArray<TimestampAdjuster> timestampAdjusters;

    public TimestampAdjusterProvider() {
        timestampAdjusters = new SparseArray<>();
    }

    /**
     * Returns a {@link TimestampAdjuster} suitable for adjusting the pts timestamps contained in
     * a chunk with a given discontinuity sequence.
     *
     * @param discontinuitySequence The chunk's discontinuity sequence.
     * @return A {@link TimestampAdjuster}.
     */
    public TimestampAdjuster getAdjuster(int discontinuitySequence) {
        TimestampAdjuster adjuster = timestampAdjusters.get(discontinuitySequence);
        if (adjuster == null) {
            adjuster = new TimestampAdjuster(TimestampAdjuster.DO_NOT_OFFSET);
            timestampAdjusters.put(discontinuitySequence, adjuster);
        }
        return adjuster;
    }

    /**
     * Resets the provider.
     */
    public void reset() {
        timestampAdjusters.clear();
    }

}