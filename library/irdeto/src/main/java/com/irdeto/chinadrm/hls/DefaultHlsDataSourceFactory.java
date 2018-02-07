package com.irdeto.chinadrm.hls;

import com.google.android.exoplayer2.upstream.DataSource;

/**
 * Default implementation of {@link HlsDataSourceFactory}.
 */
public final class DefaultHlsDataSourceFactory implements HlsDataSourceFactory {

    private final DataSource.Factory dataSourceFactory;

    /**
     * @param dataSourceFactory The {@link DataSource.Factory} to use for all data types.
     */
    public DefaultHlsDataSourceFactory(DataSource.Factory dataSourceFactory) {
        this.dataSourceFactory = dataSourceFactory;
    }

    @Override
    public DataSource createDataSource(int dataType) {
        return dataSourceFactory.createDataSource();
    }

}
