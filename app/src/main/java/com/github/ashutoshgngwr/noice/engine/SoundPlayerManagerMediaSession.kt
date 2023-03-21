package com.github.ashutoshgngwr.noice.engine

import android.app.PendingIntent
import android.content.Context
import android.media.AudioManager
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.media.VolumeProviderCompat
import androidx.mediarouter.media.MediaRouter
import com.github.ashutoshgngwr.noice.R

/**
 * A convenient wrapper around [MediaSessionCompat].
 */
class SoundPlayerManagerMediaSession(context: Context, sessionActivityPi: PendingIntent) {

  private var callback: Callback? = null
  private var audioStream = AudioManager.STREAM_MUSIC
  private var isPlaybackLocal = false
  private val defaultTitle = context.getString(R.string.unsaved_preset)
  private val mediaSession = MediaSessionCompat(context, "${context.packageName}:mediaSession")
  private val playbackStateBuilder = PlaybackStateCompat.Builder()
    .setActions(
      PlaybackStateCompat.ACTION_PLAY_PAUSE
        or PlaybackStateCompat.ACTION_PAUSE
        or PlaybackStateCompat.ACTION_STOP
        or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
        or PlaybackStateCompat.ACTION_SKIP_TO_NEXT
    )

  init {
    mediaSession.setSessionActivity(sessionActivityPi)
    mediaSession.setCallback(object : MediaSessionCompat.Callback() {
      override fun onPlay() {
        callback?.onPlay()
      }

      override fun onStop() {
        callback?.onStop()
      }

      override fun onPause() {
        callback?.onPause()
      }

      override fun onSkipToPrevious() {
        callback?.onSkipToPrevious()
      }

      override fun onSkipToNext() {
        callback?.onSkipToNext()
      }
    })

    setPlaybackToLocal()
    mediaSession.isActive = true
    MediaRouter.getInstance(context).setMediaSessionCompat(mediaSession)
  }

  /**
   * Returns the media session token ([MediaSessionCompat.getSessionToken]).
   */
  fun getSessionToken(): MediaSessionCompat.Token {
    return mediaSession.sessionToken
  }

  /**
   * Sets the audio stream for this media session to update the volume handling.
   */
  fun setAudioStream(stream: Int) {
    audioStream = stream
    if (isPlaybackLocal) {
      mediaSession.setPlaybackToLocal(stream)
    }
  }

  /**
   * Configures this session to use local volume handling based on the last audio stream configured
   * using [setAudioStream].
   */
  fun setPlaybackToLocal() {
    isPlaybackLocal = true
    mediaSession.setPlaybackToLocal(audioStream)
  }

  /**
   * Configures this session to use remote volume handling.
   */
  fun setPlaybackToRemote(volumeProvider: VolumeProviderCompat) {
    isPlaybackLocal = false
    mediaSession.setPlaybackToRemote(volumeProvider)
  }

  /**
   * Translates the given [state] to the [PlaybackStateCompat] and sets it on the current media
   * session.
   */
  fun setState(state: SoundPlayerManager.State) {
    when (state) {
      SoundPlayerManager.State.STOPPED -> playbackStateBuilder.setState(
        PlaybackStateCompat.STATE_STOPPED,
        PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN,
        0f,
      )

      SoundPlayerManager.State.PAUSED -> playbackStateBuilder.setState(
        PlaybackStateCompat.STATE_PAUSED,
        PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN,
        0f,
      )

      else -> playbackStateBuilder.setState(
        PlaybackStateCompat.STATE_PLAYING,
        PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN,
        1f,
      )
    }

    mediaSession.setPlaybackState(playbackStateBuilder.build())
  }

  /**
   * Adds the given [name] to the media session's metadata. If [name] is `null`, it uses
   * [R.string.unsaved_preset] as fallback.
   */
  fun setCurrentPresetName(name: String?) {
    mediaSession.setMetadata(
      MediaMetadataCompat.Builder()
        .putString(MediaMetadataCompat.METADATA_KEY_TITLE, name ?: defaultTitle)
        .build()
    )
  }

  /**
   * Adds a callback to receive updates on for the MediaSession.
   *
   * @see MediaSessionCompat.setCallback
   */
  fun setCallback(callback: Callback?) {
    this.callback = callback
  }

  /**
   * Releases the underlying media session.
   */
  fun release() {
    mediaSession.release()
  }

  /**
   * Receives transport controls, media buttons, and commands from controllers and the system. The
   * callback may be set using setCallback. It is wrapped in a [MediaSessionCompat.Callback] under
   * the hood.
   */
  interface Callback {
    fun onPlay()
    fun onStop()
    fun onPause()
    fun onSkipToPrevious()
    fun onSkipToNext()
  }
}
