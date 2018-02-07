package com.irdeto.chinadrm.hls;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.upstream.DataSource;

/**
 * Creates {@link DataSource}s for HLS playlists, encryption and media chunks.
 */
public interface HlsDataSourceFactory {

    /**
     * Creates a {@link DataSource} for the given data type.
     *
     * @param dataType The data type for which the {@link DataSource} will be used. One of {@link C}
     *     {@code .DATA_TYPE_*} constants.
     * @return A {@link DataSource} for the given data type.
     */
    DataSource createDataSource(int dataType);

}