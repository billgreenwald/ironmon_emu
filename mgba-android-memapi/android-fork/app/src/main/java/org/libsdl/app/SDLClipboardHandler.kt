package org.libsdl.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ClipboardManager.OnPrimaryClipChangedListener
import android.content.Context
import org.libsdl.app.SDL.getContext

class SDLClipboardHandler : OnPrimaryClipChangedListener {
    protected var mClipMgr: ClipboardManager

    init {
        mClipMgr = getContext()!!.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        mClipMgr.addPrimaryClipChangedListener(this)
    }

    fun clipboardHasText(): Boolean {
        return mClipMgr.hasPrimaryClip()
    }

    fun clipboardGetText(): String? {
        val clip = mClipMgr.primaryClip
        if (clip != null) {
            val item = clip.getItemAt(0)
            if (item != null) {
                val text = item.text
                if (text != null) {
                    return text.toString()
                }
            }
        }
        return null
    }

    fun clipboardSetText(string: String?) {
        mClipMgr.removePrimaryClipChangedListener(this)
        val clip = ClipData.newPlainText(null, string)
        mClipMgr.setPrimaryClip(clip)
        mClipMgr.addPrimaryClipChangedListener(this)
    }

    override fun onPrimaryClipChanged() {
        SDLActivity.onNativeClipboardChanged()
    }
}
