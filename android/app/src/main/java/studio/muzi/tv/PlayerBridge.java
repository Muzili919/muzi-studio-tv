package studio.muzi.tv;

import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.PlaybackParameters;
import androidx.media3.common.Player;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;

/**
 * ExoPlayer (default) or IJK. Exo buffer sizes stay at the existing 60s/180s/64MB settings.
 * Thunder/Xunlei is not linked.
 */
final class PlayerBridge {
    static final int STATE_IDLE = Player.STATE_IDLE;
    static final int STATE_BUFFERING = Player.STATE_BUFFERING;
    static final int STATE_READY = Player.STATE_READY;
    static final int STATE_ENDED = Player.STATE_ENDED;

    interface Listener {
        void onPlaybackStateChanged(int state);

        void onPlayerError(String code);
    }

    final boolean fellBackToExo;
    private final MainActivity activity;
    private final String engine;
    private final Listener listener;
    private final TextureView texture;
    private ExoPlayer exo;
    private IjkMediaPlayer ijk;
    private Surface ijkSurface;
    private int state = STATE_IDLE;
    private boolean playWhenReady = true;
    private boolean seekable = true;
    private int bufferedPercent;
    private long bufferedMs;
    private float speed = 1f;
    private boolean released;

    private PlayerBridge(MainActivity activity, String engine, TextureView texture, Listener listener, boolean fellBackToExo) {
        this.activity = activity;
        this.engine = engine;
        this.texture = texture;
        this.listener = listener;
        this.fellBackToExo = fellBackToExo;
    }

    static PlayerBridge open(MainActivity activity, String wantEngine, TextureView texture, Map<String, String> headers, String url, long seekMs, float speed, Listener listener) {
        boolean ijk = "ijk".equals(wantEngine);
        boolean fellBack = false;
        if (ijk && !ijkAvailable()) {
            Log.w(MainActivity.TAG, "IJK load failed, fallback Exo");
            ijk = false;
            fellBack = true;
        }
        PlayerBridge bridge = new PlayerBridge(activity, ijk ? "ijk" : "exo", texture, listener, fellBack);
        bridge.speed = speed <= 0f ? 1f : speed;
        if (ijk) {
            bridge.openIjk(url, headers, seekMs);
        } else {
            bridge.openExo(url, headers, seekMs);
        }
        return bridge;
    }

    static boolean ijkAvailable() {
        try {
            IjkMediaPlayer.loadLibrariesOnce(null);
            return true;
        } catch (Throwable t) {
            Log.w(MainActivity.TAG, "ijk load", t);
            return false;
        }
    }

    String engineName() {
        return engine;
    }

    private void openExo(String url, Map<String, String> headers, long seekMs) {
        Map<String, String> safe = headers == null ? new HashMap<String, String>() : headers;
        DefaultHttpDataSource.Factory http = new DefaultHttpDataSource.Factory().setUserAgent("MuziTV/1.1").setDefaultRequestProperties(safe).setConnectTimeoutMs(15000).setReadTimeoutMs(25000);
        // 缓冲维持现状：60s min / 180s max / 64MB，不再加大。
        DefaultLoadControl load = new DefaultLoadControl.Builder().setBufferDurationsMs(60000, 180000, DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS, 10000).setTargetBufferBytes(67108864).setPrioritizeTimeOverSizeThresholds(false).build();
        exo = new ExoPlayer.Builder(activity).setMediaSourceFactory(new DefaultMediaSourceFactory(http)).setLoadControl(load).build();
        exo.setVideoTextureView(texture);
        exo.setAudioAttributes(new AudioAttributes.Builder().setUsage(1).setContentType(3).build(), true);
        exo.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                state = playbackState;
                if (playbackState == STATE_READY || playbackState == STATE_BUFFERING) {
                    bufferedMs = exo == null ? 0 : exo.getTotalBufferedDuration();
                    bufferedPercent = exo == null ? 0 : exo.getBufferedPercentage();
                }
                emitState(playbackState);
            }

            @Override
            public void onPlayerError(PlaybackException error) {
                state = STATE_IDLE;
                emitError(error.getErrorCodeName());
            }
        });
        MediaItem.Builder media = new MediaItem.Builder().setUri(url);
        String mime = mimeOf(url);
        if (mime != null) {
            media.setMimeType(mime);
        }
        exo.setMediaItem(media.build());
        exo.setPlaybackParameters(new PlaybackParameters(speed));
        if (seekMs > 0) {
            exo.seekTo(seekMs);
        }
        state = STATE_BUFFERING;
        emitState(STATE_BUFFERING);
        exo.prepare();
        if (playWhenReady) {
            exo.play();
        }
    }

    private void openIjk(String url, Map<String, String> headers, final long seekMs) {
        ijk = new IjkMediaPlayer();
        ijk.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec", 1);
        ijk.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-auto-rotate", 1);
        ijk.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-handle-resolution-change", 1);
        ijk.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "opensles", 0);
        ijk.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "overlay-format", IjkMediaPlayer.SDL_FCC_RV32);
        ijk.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "framedrop", 1);
        ijk.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "start-on-prepared", 0);
        ijk.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "timeout", 15000000);
        ijk.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "reconnect", 1);
        ijk.setOption(IjkMediaPlayer.OPT_CATEGORY_CODEC, "skip_loop_filter", 48);
        ijk.setOnPreparedListener(new IMediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(IMediaPlayer mp) {
                state = STATE_READY;
                try {
                    if (seekMs > 0) {
                        mp.seekTo(seekMs);
                    }
                    ijk.setSpeed(speed);
                } catch (Throwable ignored) {
                }
                if (playWhenReady) {
                    try {
                        mp.start();
                    } catch (Throwable ignored) {
                    }
                }
                emitState(STATE_READY);
            }
        });
        ijk.setOnCompletionListener(new IMediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(IMediaPlayer mp) {
                state = STATE_ENDED;
                emitState(STATE_ENDED);
            }
        });
        ijk.setOnErrorListener(new IMediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(IMediaPlayer mp, int what, int extra) {
                state = STATE_IDLE;
                emitError("IJK_" + what + "_" + extra);
                return true;
            }
        });
        ijk.setOnInfoListener(new IMediaPlayer.OnInfoListener() {
            @Override
            public boolean onInfo(IMediaPlayer mp, int what, int extra) {
                if (what == IMediaPlayer.MEDIA_INFO_BUFFERING_START) {
                    state = STATE_BUFFERING;
                    emitState(STATE_BUFFERING);
                } else if (what == IMediaPlayer.MEDIA_INFO_BUFFERING_END) {
                    state = STATE_READY;
                    emitState(STATE_READY);
                } else if (what == IMediaPlayer.MEDIA_INFO_NOT_SEEKABLE) {
                    seekable = false;
                }
                return false;
            }
        });
        ijk.setOnBufferingUpdateListener(new IMediaPlayer.OnBufferingUpdateListener() {
            @Override
            public void onBufferingUpdate(IMediaPlayer mp, int percent) {
                bufferedPercent = percent;
            }
        });
        try {
            ijk.setScreenOnWhilePlaying(true);
            if (headers != null && !headers.isEmpty()) {
                ijk.setDataSource(url, headers);
            } else {
                ijk.setDataSource(url);
            }
            bindIjkSurface();
            state = STATE_BUFFERING;
            emitState(STATE_BUFFERING);
            ijk.prepareAsync();
        } catch (Throwable t) {
            Log.e(MainActivity.TAG, "ijk open", t);
            emitError(t.getClass().getSimpleName());
        }
    }

    private void bindIjkSurface() {
        if (texture == null || ijk == null) {
            return;
        }
        if (texture.getSurfaceTexture() != null) {
            attachSurface(texture.getSurfaceTexture());
        }
        texture.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(android.graphics.SurfaceTexture surface, int width, int height) {
                attachSurface(surface);
            }

            @Override
            public void onSurfaceTextureSizeChanged(android.graphics.SurfaceTexture surface, int width, int height) {
            }

            @Override
            public boolean onSurfaceTextureDestroyed(android.graphics.SurfaceTexture surface) {
                if (ijk != null) {
                    ijk.setSurface(null);
                }
                return true;
            }

            @Override
            public void onSurfaceTextureUpdated(android.graphics.SurfaceTexture surface) {
            }
        });
    }

    private void attachSurface(android.graphics.SurfaceTexture surface) {
        if (ijk == null || surface == null) {
            return;
        }
        if (ijkSurface != null) {
            ijkSurface.release();
        }
        ijkSurface = new Surface(surface);
        ijk.setSurface(ijkSurface);
    }

    private void emitError(final String code) {
        if (listener == null) {
            return;
        }
        activity.ui.post(new Runnable() {
            @Override
            public void run() {
                if (!released && listener != null) {
                    listener.onPlayerError(code);
                }
            }
        });
    }

    private void emitState(final int playbackState) {
        if (listener == null) {
            return;
        }
        activity.ui.post(new Runnable() {
            @Override
            public void run() {
                if (!released && listener != null) {
                    listener.onPlaybackStateChanged(playbackState);
                }
            }
        });
    }

    private static String mimeOf(String url) {
        String lower = url == null ? "" : url.toLowerCase(Locale.ROOT);
        if (lower.contains("m3u8")) {
            return MimeTypes.APPLICATION_M3U8;
        }
        if (lower.contains(".mp4")) {
            return MimeTypes.VIDEO_MP4;
        }
        return null;
    }

    void play() {
        playWhenReady = true;
        try {
            if (exo != null) {
                exo.play();
            } else if (ijk != null) {
                ijk.start();
            }
        } catch (Throwable ignored) {
        }
    }

    void pause() {
        playWhenReady = false;
        try {
            if (exo != null) {
                exo.pause();
            } else if (ijk != null) {
                ijk.pause();
            }
        } catch (Throwable ignored) {
        }
    }

    void seekTo(long ms) {
        try {
            if (exo != null) {
                exo.seekTo(ms);
            } else if (ijk != null) {
                ijk.seekTo(ms);
            }
        } catch (Throwable ignored) {
        }
    }

    void setSpeed(float next) {
        speed = next <= 0f ? 1f : next;
        try {
            if (exo != null) {
                exo.setPlaybackParameters(new PlaybackParameters(speed));
            } else if (ijk != null) {
                ijk.setSpeed(speed);
            }
        } catch (Throwable ignored) {
        }
    }

    boolean isPlaying() {
        try {
            if (exo != null) {
                return exo.isPlaying();
            }
            return ijk != null && ijk.isPlaying();
        } catch (Throwable ignored) {
            return false;
        }
    }

    long getCurrentPosition() {
        try {
            if (exo != null) {
                return exo.getCurrentPosition();
            }
            return ijk == null ? 0L : ijk.getCurrentPosition();
        } catch (Throwable ignored) {
            return 0L;
        }
    }

    long getDuration() {
        try {
            if (exo != null) {
                return exo.getDuration();
            }
            return ijk == null ? 0L : ijk.getDuration();
        } catch (Throwable ignored) {
            return 0L;
        }
    }

    boolean isCurrentMediaItemSeekable() {
        try {
            if (exo != null) {
                return exo.isCurrentMediaItemSeekable();
            }
            return seekable && getDuration() > 0;
        } catch (Throwable ignored) {
            return false;
        }
    }

    int getBufferedPercentage() {
        try {
            if (exo != null) {
                return exo.getBufferedPercentage();
            }
        } catch (Throwable ignored) {
        }
        return bufferedPercent;
    }

    long getTotalBufferedDuration() {
        try {
            if (exo != null) {
                return exo.getTotalBufferedDuration();
            }
            if (ijk != null) {
                bufferedMs = ijk.getVideoCachedDuration();
            }
        } catch (Throwable ignored) {
        }
        return bufferedMs;
    }

    float getSpeed() {
        return speed;
    }

    int getPlaybackState() {
        if (exo != null) {
            try {
                return exo.getPlaybackState();
            } catch (Throwable ignored) {
                return state;
            }
        }
        return state;
    }

    boolean getPlayWhenReady() {
        if (exo != null) {
            try {
                return exo.getPlayWhenReady();
            } catch (Throwable ignored) {
            }
        }
        return playWhenReady;
    }

    String audioLabel() {
        try {
            if (exo != null && exo.getAudioFormat() != null) {
                return exo.getAudioFormat().sampleMimeType;
            }
        } catch (Throwable ignored) {
        }
        return "ijk".equals(engine) ? "ijk" : "none";
    }

    void release() {
        released = true;
        try {
            if (exo != null) {
                exo.release();
            }
        } catch (Throwable ignored) {
        }
        exo = null;
        try {
            if (ijk != null) {
                ijk.setSurface(null);
                ijk.release();
            }
        } catch (Throwable ignored) {
        }
        ijk = null;
        if (ijkSurface != null) {
            ijkSurface.release();
            ijkSurface = null;
        }
        if (texture != null) {
            texture.setSurfaceTextureListener(null);
        }
    }
}
