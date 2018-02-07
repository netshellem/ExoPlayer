package com.irdeto.chinadrm.hls;

import com.irdeto.chinadrm.hls.playlist.HlsMasterPlaylist;
import com.irdeto.chinadrm.hls.playlist.HlsMediaPlaylist;

/**
 * Holds a master playlist along with a snapshot of one of its media playlists.
 */
public final class HlsManifest {

    /**
     * The master playlist of an HLS stream.
     */
    public final HlsMasterPlaylist masterPlaylist;
    /**
     * A snapshot of a media playlist referred to by {@link #masterPlaylist}.
     */
    public final HlsMediaPlaylist mediaPlaylist;

    /**
     * @param masterPlaylist The master playlist.
     * @param mediaPlaylist The media playlist.
     */
    HlsManifest(HlsMasterPlaylist masterPlaylist, HlsMediaPlaylist mediaPlaylist) {
        this.masterPlaylist = masterPlaylist;
        this.mediaPlaylist = mediaPlaylist;
    }

}