package org.libsdl.app

import android.content.Context
import android.text.InputType
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection

/* This is a fake invisible editor view that receives the input and defines the
 * pan&scan region
 */
class DummyEdit(context: Context?) : View(context), View.OnKeyListener {
    lateinit var ic: InputConnection

    init {
        isFocusableInTouchMode = true
        isFocusable = true
        setOnKeyListener(this)
    }

    override fun onCheckIsTextEditor(): Boolean {
        return true
    }

    override fun onKey(v: View, keyCode: Int, event: KeyEvent): Boolean {
        return SDLActivity.handleKeyEvent(v, keyCode, event, ic)
    }

    //
    override fun onKeyPreIme(keyCode: Int, event: KeyEvent): Boolean {
        // As seen on StackOverflow: http://stackoverflow.com/questions/7634346/keyboard-hide-event
        // FIXME: Discussion at http://bugzilla.libsdl.org/show_bug.cgi?id=1639
        // FIXME: This is not a 100% effective solution to the problem of detecting if the keyboard is showing or not
        // FIXME: A more effective solution would be to assume our Layout to be RelativeLayout or LinearLayout
        // FIXME: And determine the keyboard presence doing this: http://stackoverflow.com/questions/2150078/how-to-check-visibility-of-software-keyboard-in-android
        // FIXME: An even more effective way would be if Android provided this out of the box, but where would the fun be in that :)
        if (event.action == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_BACK) {
            if (SDLActivity.mTextEdit != null && SDLActivity.mTextEdit!!.visibility == VISIBLE) {
                SDLActivity.onNativeKeyboardFocusLost()
            }
        }
        return super.onKeyPreIme(keyCode, event)
    }

    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection {
        ic = SDLInputConnection(this, true)
        outAttrs.inputType = InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_FLAG_MULTI_LINE
        outAttrs.imeOptions = EditorInfo.IME_FLAG_NO_EXTRACT_UI or
                EditorInfo.IME_FLAG_NO_FULLSCREEN /* API 11 */
        return ic
    }
}
