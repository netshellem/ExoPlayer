package com.irdeto.chinadrm.hls;

import android.util.Log;
import android.net.Uri;
import android.os.Handler;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerLibraryInfo;
import com.google.android.exoplayer2.source.AdaptiveMediaSourceEventListener;
import com.google.android.exoplayer2.source.AdaptiveMediaSourceEventListener.EventDispatcher;
import com.google.android.exoplayer2.source.MediaPeriod;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.SinglePeriodTimeline;
import com.google.android.exoplayer2.util.UriUtil;
import com.irdeto.chinadrm.hls.playlist.HlsMediaPlaylist;
import com.irdeto.chinadrm.hls.playlist.HlsPlaylist;
import com.irdeto.chinadrm.hls.playlist.HlsPlaylistParser;
import com.irdeto.chinadrm.hls.playlist.HlsPlaylistTracker;
import com.google.android.exoplayer2.upstream.Allocator;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.ParsingLoadable;
import com.google.android.exoplayer2.util.Assertions;
import java.io.IOException;
import java.util.List;
import com.irdeto.drm.ChinaDrm;
import com.irdeto.drm.session.Session;
import com.irdeto.drm.session.SessionManager;
import com.irdeto.drm.session.UserService;

/**
 * An HLS {@link MediaSource}.
 */
public final class HlsMediaSource implements MediaSource,
        HlsPlaylistTracker.PrimaryPlaylistListener {



    /**
     * The default minimum number of times to retry loading data prior to failing.
     */
    static {
        ExoPlayerLibraryInfo.registerModule("com.irdeto.chinadrm.hls");
    }

    public static final int DEFAULT_MIN_LOADABLE_RETRY_COUNT = 3;

    private final Uri manifestUri;
    private final HlsDataSourceFactory dataSourceFactory;
    private final int minLoadableRetryCount;
    private final EventDispatcher eventDispatcher;
    private final ParsingLoadable.Parser<HlsPlaylist> playlistParser;
    private final ChinaDrm drm;

    private HlsPlaylistTracker playlistTracker;
    private Listener sourceListener;

    public HlsMediaSource(Uri manifestUri, DataSource.Factory dataSourceFactory, Handler eventHandler,
                          AdaptiveMediaSourceEventListener eventListener, ChinaDrm drm) {
        this(manifestUri, dataSourceFactory, DEFAULT_MIN_LOADABLE_RETRY_COUNT, eventHandler,
                eventListener, drm);
    }

    public HlsMediaSource(Uri manifestUri, DataSource.Factory dataSourceFactory,
                          int minLoadableRetryCount, Handler eventHandler,
                          AdaptiveMediaSourceEventListener eventListener, ChinaDrm drm) {
        this(manifestUri, new DefaultHlsDataSourceFactory(dataSourceFactory), minLoadableRetryCount,
                eventHandler, eventListener, drm);
    }

    public HlsMediaSource(Uri manifestUri, HlsDataSourceFactory dataSourceFactory,
                          int minLoadableRetryCount, Handler eventHandler,
                          AdaptiveMediaSourceEventListener eventListener, ChinaDrm drm) {
        this(manifestUri, dataSourceFactory, minLoadableRetryCount, eventHandler, eventListener,
                new HlsPlaylistParser(), drm);
    }

    public HlsMediaSource(Uri manifestUri, HlsDataSourceFactory dataSourceFactory,
                          int minLoadableRetryCount, Handler eventHandler,
                          AdaptiveMediaSourceEventListener eventListener,
                          ParsingLoadable.Parser<HlsPlaylist> playlistParser, ChinaDrm drm) {
        this.manifestUri = manifestUri;
        this.dataSourceFactory = dataSourceFactory;
        this.minLoadableRetryCount = minLoadableRetryCount;
        this.playlistParser = playlistParser;
        eventDispatcher = new EventDispatcher(eventHandler, eventListener);
        this.drm = drm;
        //acquireLicenseByUrl();

    }



    @Override
    public void prepareSource(ExoPlayer player, boolean isTopLevelSource, Listener listener) {
        Assertions.checkState(playlistTracker == null);
        playlistTracker = new HlsPlaylistTracker(manifestUri, dataSourceFactory, eventDispatcher,
                minLoadableRetryCount, this, playlistParser);
        sourceListener = listener;
        playlistTracker.start();

    }

    @Override
    public void maybeThrowSourceInfoRefreshError() throws IOException {
        playlistTracker.maybeThrowPrimaryPlaylistRefreshError();
    }

    @Override
    public MediaPeriod createPeriod(MediaPeriodId id, Allocator allocator) {
        Assertions.checkArgument(id.periodIndex == 0);
        return new HlsMediaPeriod(playlistTracker, dataSourceFactory, minLoadableRetryCount,
                eventDispatcher, allocator, this.drm);
    }

    @Override
    public void releasePeriod(MediaPeriod mediaPeriod) {
        ((HlsMediaPeriod) mediaPeriod).release();
    }

    @Override
    public void releaseSource() {
        if (playlistTracker != null) {
            playlistTracker.release();
            playlistTracker = null;
        }
        sourceListener = null;
    }

    @Override
    public void onPrimaryPlaylistRefreshed(HlsMediaPlaylist playlist) {
        SinglePeriodTimeline timeline;
       // System.out.println("***");
        long presentationStartTimeMs = playlist.hasProgramDateTime ? 0 : C.TIME_UNSET;
        long windowStartTimeMs = playlist.hasProgramDateTime ? C.usToMs(playlist.startTimeUs)
                : C.TIME_UNSET;
        long windowDefaultStartPositionUs = playlist.startOffsetUs;
        if (playlistTracker.isLive()) {
            long periodDurationUs = playlist.hasEndTag ? (playlist.startTimeUs + playlist.durationUs)
                    : C.TIME_UNSET;
            List<HlsMediaPlaylist.Segment> segments = playlist.segments;
            if (windowDefaultStartPositionUs == C.TIME_UNSET) {
                windowDefaultStartPositionUs = segments.isEmpty() ? 0
                        : segments.get(Math.max(0, segments.size() - 3)).relativeStartTimeUs;
            }
            timeline = new SinglePeriodTimeline(presentationStartTimeMs, windowStartTimeMs,
                    periodDurationUs, playlist.durationUs, playlist.startTimeUs, windowDefaultStartPositionUs,
                    true, !playlist.hasEndTag);
        } else /* not live */ {
            if (windowDefaultStartPositionUs == C.TIME_UNSET) {
                windowDefaultStartPositionUs = 0;
            }
            timeline = new SinglePeriodTimeline(presentationStartTimeMs, windowStartTimeMs,
                    playlist.startTimeUs + playlist.durationUs, playlist.durationUs, playlist.startTimeUs,
                    windowDefaultStartPositionUs, true, false);
        }
        sourceListener.onSourceInfoRefreshed(timeline,
                new HlsManifest(playlistTracker.getMasterPlaylist(), playlist));
    }

}
