package org.libsdl.app

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.os.Build
import android.os.Process
import android.util.Log

object SDLAudioManager {
    internal const val TAG = "SDLAudio"
    internal var mAudioTrack: AudioTrack? = null
    internal var mAudioRecord: AudioRecord? = null
    fun initialize() {
        mAudioTrack = null
        mAudioRecord = null
    }

    // Audio
    internal fun getAudioFormatString(audioFormat: Int): String {
        return when (audioFormat) {
            AudioFormat.ENCODING_PCM_8BIT -> "8-bit"
            AudioFormat.ENCODING_PCM_16BIT -> "16-bit"
            AudioFormat.ENCODING_PCM_FLOAT -> "float"
            else -> Integer.toString(audioFormat)
        }
    }

    @SuppressLint("MissingPermission")
    internal fun open(
        isCapture: Boolean,
        sampleRate: Int,
        audioFormat: Int,
        desiredChannels: Int,
        desiredFrames: Int
    ): IntArray? {
        var sampleRate = sampleRate
        var audioFormat = audioFormat
        var desiredChannels = desiredChannels
        var desiredFrames = desiredFrames
        val channelConfig: Int
        val sampleSize: Int
        val frameSize: Int
        Log.v(
            TAG,
            "Opening " + (if (isCapture) "capture" else "playback") + ", requested " + desiredFrames + " frames of " + desiredChannels + " channel " + getAudioFormatString(
                audioFormat
            ) + " audio at " + sampleRate + " Hz"
        )

        /* On older devices let's use known good settings */if (Build.VERSION.SDK_INT < 21) {
            if (desiredChannels > 2) {
                desiredChannels = 2
            }
        }

        /* AudioTrack has sample rate limitation of 48000 (fixed in 5.0.2) */if (Build.VERSION.SDK_INT < 22) {
            if (sampleRate < 8000) {
                sampleRate = 8000
            } else if (sampleRate > 48000) {
                sampleRate = 48000
            }
        }
        if (audioFormat == AudioFormat.ENCODING_PCM_FLOAT) {
            val minSDKVersion = if (isCapture) 23 else 21
            if (Build.VERSION.SDK_INT < minSDKVersion) {
                audioFormat = AudioFormat.ENCODING_PCM_16BIT
            }
        }
        when (audioFormat) {
            AudioFormat.ENCODING_PCM_8BIT -> sampleSize = 1
            AudioFormat.ENCODING_PCM_16BIT -> sampleSize = 2
            AudioFormat.ENCODING_PCM_FLOAT -> sampleSize = 4
            else -> {
                Log.v(TAG, "Requested format $audioFormat, getting ENCODING_PCM_16BIT")
                audioFormat = AudioFormat.ENCODING_PCM_16BIT
                sampleSize = 2
            }
        }
        if (isCapture) {
            when (desiredChannels) {
                1 -> channelConfig = AudioFormat.CHANNEL_IN_MONO
                2 -> channelConfig = AudioFormat.CHANNEL_IN_STEREO
                else -> {
                    Log.v(TAG, "Requested $desiredChannels channels, getting stereo")
                    desiredChannels = 2
                    channelConfig = AudioFormat.CHANNEL_IN_STEREO
                }
            }
        } else {
            when (desiredChannels) {
                1 -> channelConfig = AudioFormat.CHANNEL_OUT_MONO
                2 -> channelConfig = AudioFormat.CHANNEL_OUT_STEREO
                3 -> channelConfig =
                    AudioFormat.CHANNEL_OUT_STEREO or AudioFormat.CHANNEL_OUT_FRONT_CENTER

                4 -> channelConfig = AudioFormat.CHANNEL_OUT_QUAD
                5 -> channelConfig =
                    AudioFormat.CHANNEL_OUT_QUAD or AudioFormat.CHANNEL_OUT_FRONT_CENTER

                6 -> channelConfig = AudioFormat.CHANNEL_OUT_5POINT1
                7 -> channelConfig =
                    AudioFormat.CHANNEL_OUT_5POINT1 or AudioFormat.CHANNEL_OUT_BACK_CENTER

                8 -> if (Build.VERSION.SDK_INT >= 23) {
                    channelConfig = AudioFormat.CHANNEL_OUT_7POINT1_SURROUND
                } else {
                    Log.v(TAG, "Requested $desiredChannels channels, getting 5.1 surround")
                    desiredChannels = 6
                    channelConfig = AudioFormat.CHANNEL_OUT_5POINT1
                }

                else -> {
                    Log.v(TAG, "Requested $desiredChannels channels, getting stereo")
                    desiredChannels = 2
                    channelConfig = AudioFormat.CHANNEL_OUT_STEREO
                }
            }

            /*
            Log.v(TAG, "Speaker configuration (and order of channels):");

            if ((channelConfig & 0x00000004) != 0) {
                Log.v(TAG, "   CHANNEL_OUT_FRONT_LEFT");
            }
            if ((channelConfig & 0x00000008) != 0) {
                Log.v(TAG, "   CHANNEL_OUT_FRONT_RIGHT");
            }
            if ((channelConfig & 0x00000010) != 0) {
                Log.v(TAG, "   CHANNEL_OUT_FRONT_CENTER");
            }
            if ((channelConfig & 0x00000020) != 0) {
                Log.v(TAG, "   CHANNEL_OUT_LOW_FREQUENCY");
            }
            if ((channelConfig & 0x00000040) != 0) {
                Log.v(TAG, "   CHANNEL_OUT_BACK_LEFT");
            }
            if ((channelConfig & 0x00000080) != 0) {
                Log.v(TAG, "   CHANNEL_OUT_BACK_RIGHT");
            }
            if ((channelConfig & 0x00000100) != 0) {
                Log.v(TAG, "   CHANNEL_OUT_FRONT_LEFT_OF_CENTER");
            }
            if ((channelConfig & 0x00000200) != 0) {
                Log.v(TAG, "   CHANNEL_OUT_FRONT_RIGHT_OF_CENTER");
            }
            if ((channelConfig & 0x00000400) != 0) {
                Log.v(TAG, "   CHANNEL_OUT_BACK_CENTER");
            }
            if ((channelConfig & 0x00000800) != 0) {
                Log.v(TAG, "   CHANNEL_OUT_SIDE_LEFT");
            }
            if ((channelConfig & 0x00001000) != 0) {
                Log.v(TAG, "   CHANNEL_OUT_SIDE_RIGHT");
            }
*/
        }
        frameSize = sampleSize * desiredChannels

        // Let the user pick a larger buffer if they really want -- but ye
        // gods they probably shouldn't, the minimums are horrifyingly high
        // latency already
        val minBufferSize: Int
        minBufferSize = if (isCapture) {
            AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        } else {
            AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        }
        desiredFrames = Math.max(desiredFrames, (minBufferSize + frameSize - 1) / frameSize)
        val results = IntArray(4)
        if (isCapture) {
            if (mAudioRecord == null) {
                mAudioRecord = AudioRecord(
                    MediaRecorder.AudioSource.DEFAULT, sampleRate,
                    channelConfig, audioFormat, desiredFrames * frameSize
                )

                // see notes about AudioTrack state in audioOpen(), above. Probably also applies here.
                if (mAudioRecord!!.state != AudioRecord.STATE_INITIALIZED) {
                    Log.e(TAG, "Failed during initialization of AudioRecord")
                    mAudioRecord!!.release()
                    mAudioRecord = null
                    return null
                }
                mAudioRecord!!.startRecording()
            }
            results[0] = mAudioRecord!!.sampleRate
            results[1] = mAudioRecord!!.audioFormat
            results[2] = mAudioRecord!!.channelCount
        } else {
            if (mAudioTrack == null) {
                mAudioTrack = AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    sampleRate,
                    channelConfig,
                    audioFormat,
                    desiredFrames * frameSize,
                    AudioTrack.MODE_STREAM
                )

                // Instantiating AudioTrack can "succeed" without an exception and the track may still be invalid
                // Ref: https://android.googlesource.com/platform/frameworks/base/+/refs/heads/master/media/java/android/media/AudioTrack.java
                // Ref: http://developer.android.com/reference/android/media/AudioTrack.html#getState()
                if (mAudioTrack!!.state != AudioTrack.STATE_INITIALIZED) {
                    /* Try again, with safer values */
                    Log.e(TAG, "Failed during initialization of Audio Track")
                    mAudioTrack!!.release()
                    mAudioTrack = null
                    return null
                }
                mAudioTrack!!.play()
            }
            results[0] = mAudioTrack!!.sampleRate
            results[1] = mAudioTrack!!.audioFormat
            results[2] = mAudioTrack!!.channelCount
        }
        results[3] = desiredFrames
        Log.v(
            TAG,
            "Opening " + (if (isCapture) "capture" else "playback") + ", got " + results[3] + " frames of " + results[2] + " channel " + getAudioFormatString(
                results[1]
            ) + " audio at " + results[0] + " Hz"
        )
        return results
    }

    /**
     * This method is called by SDL using JNI.
     */
    @JvmStatic
    fun audioOpen(
        sampleRate: Int,
        audioFormat: Int,
        desiredChannels: Int,
        desiredFrames: Int
    ): IntArray? {
        return open(false, sampleRate, audioFormat, desiredChannels, desiredFrames)
    }

    /**
     * This method is called by SDL using JNI.
     */
    @JvmStatic
    fun audioWriteFloatBuffer(buffer: FloatArray) {
        if (mAudioTrack == null) {
            Log.e(TAG, "Attempted to make audio call with uninitialized audio!")
            return
        }
        var i = 0
        while (i < buffer.size) {
            val result = mAudioTrack!!.write(buffer, i, buffer.size - i, AudioTrack.WRITE_BLOCKING)
            if (result > 0) {
                i += result
            } else if (result == 0) {
                try {
                    Thread.sleep(1)
                } catch (e: InterruptedException) {
                    // Nom nom
                }
            } else {
                Log.w(TAG, "SDL audio: error return from write(float)")
                return
            }
        }
    }

    /**
     * This method is called by SDL using JNI.
     */
    @JvmStatic
    fun audioWriteShortBuffer(buffer: ShortArray) {
        if (mAudioTrack == null) {
            Log.e(TAG, "Attempted to make audio call with uninitialized audio!")
            return
        }
        var i = 0
        while (i < buffer.size) {
            val result = mAudioTrack!!.write(buffer, i, buffer.size - i)
            if (result > 0) {
                i += result
            } else if (result == 0) {
                try {
                    Thread.sleep(1)
                } catch (e: InterruptedException) {
                    // Nom nom
                }
            } else {
                Log.w(TAG, "SDL audio: error return from write(short)")
                return
            }
        }
    }

    /**
     * This method is called by SDL using JNI.
     */
    @JvmStatic
    fun audioWriteByteBuffer(buffer: ByteArray) {
        if (mAudioTrack == null) {
            Log.e(TAG, "Attempted to make audio call with uninitialized audio!")
            return
        }
        var i = 0
        while (i < buffer.size) {
            val result = mAudioTrack!!.write(buffer, i, buffer.size - i)
            if (result > 0) {
                i += result
            } else if (result == 0) {
                try {
                    Thread.sleep(1)
                } catch (e: InterruptedException) {
                    // Nom nom
                }
            } else {
                Log.w(TAG, "SDL audio: error return from write(byte)")
                return
            }
        }
    }

    /**
     * This method is called by SDL using JNI.
     */
    @JvmStatic
    fun captureOpen(
        sampleRate: Int,
        audioFormat: Int,
        desiredChannels: Int,
        desiredFrames: Int
    ): IntArray? {
        return open(true, sampleRate, audioFormat, desiredChannels, desiredFrames)
    }

    /** This method is called by SDL using JNI.  */
    @JvmStatic
    fun captureReadFloatBuffer(buffer: FloatArray, blocking: Boolean): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mAudioRecord!!.read(
                buffer,
                0,
                buffer.size,
                if (blocking) AudioRecord.READ_BLOCKING else AudioRecord.READ_NON_BLOCKING
            )
        } else {
            return 0
        }
    }

    /** This method is called by SDL using JNI.  */
    @JvmStatic
    fun captureReadShortBuffer(buffer: ShortArray, blocking: Boolean): Int {
        return if (Build.VERSION.SDK_INT < 23) {
            mAudioRecord!!.read(buffer, 0, buffer.size)
        } else {
            mAudioRecord!!.read(
                buffer,
                0,
                buffer.size,
                if (blocking) AudioRecord.READ_BLOCKING else AudioRecord.READ_NON_BLOCKING
            )
        }
    }

    /** This method is called by SDL using JNI.  */
    @JvmStatic
    fun captureReadByteBuffer(buffer: ByteArray, blocking: Boolean): Int {
        return if (Build.VERSION.SDK_INT < 23) {
            mAudioRecord!!.read(buffer, 0, buffer.size)
        } else {
            mAudioRecord!!.read(
                buffer,
                0,
                buffer.size,
                if (blocking) AudioRecord.READ_BLOCKING else AudioRecord.READ_NON_BLOCKING
            )
        }
    }

    /** This method is called by SDL using JNI.  */
    @JvmStatic
    fun audioClose() {
        if (mAudioTrack != null) {
            mAudioTrack!!.stop()
            mAudioTrack!!.release()
            mAudioTrack = null
        }
    }

    /** This method is called by SDL using JNI.  */
    @JvmStatic
    fun captureClose() {
        if (mAudioRecord != null) {
            mAudioRecord!!.stop()
            mAudioRecord!!.release()
            mAudioRecord = null
        }
    }

    /** This method is called by SDL using JNI.  */
    @JvmStatic
    fun audioSetThreadPriority(iscapture: Boolean, device_id: Int) {
        try {

            /* Set thread name */
            if (iscapture) {
                Thread.currentThread().name = "SDLAudioC$device_id"
            } else {
                Thread.currentThread().name = "SDLAudioP$device_id"
            }

            /* Set thread priority */Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
        } catch (e: Exception) {
            Log.v(TAG, "modify thread properties failed $e")
        }
    }

    @JvmStatic
    external fun nativeSetupJNI(): Int
}
