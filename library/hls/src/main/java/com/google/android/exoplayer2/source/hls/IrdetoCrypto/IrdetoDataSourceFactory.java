package com.google.android.exoplayer2.source.hls.IrdetoCrypto;

import com.google.android.exoplayer2.upstream.DataSource;

/**
 * Created by Syliu on 19-01-2018.
 */

public class IrdetoDataSourceFactory implements DataSource.Factory {
    DataSource upstream;
    byte[] iKeyData;
    byte[] encryptionIv;
    public IrdetoDataSourceFactory(DataSource upstream,  byte[] iKeyData, byte[] encryptionIv)
    {
        this.upstream = upstream;
        this.iKeyData = iKeyData;
        this.encryptionIv = encryptionIv;
    }
    @Override
    public DataSource createDataSource() {
        return  new IrdetoAes128DataSource( upstream, iKeyData, encryptionIv) ;
    }
}
