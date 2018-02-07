package com.irdeto.chinadrm.hls.playlist;

import java.util.Collections;
import java.util.List;

/**
 * Represents an HLS playlist.
 */
public abstract class HlsPlaylist {

    /**
     * The base uri. Used to resolve relative paths.
     */
    public final String baseUri;
    /**
     * The list of tags in the playlist.
     */
    public final List<String> tags;

    /**
     * @param baseUri See {@link #baseUri}.
     * @param tags See {@link #tags}.
     */
    protected HlsPlaylist(String baseUri, List<String> tags) {
        this.baseUri = baseUri;
        this.tags = Collections.unmodifiableList(tags);
    }

}
