package org.libsdl.app

import android.text.Editable
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.BaseInputConnection
import android.widget.EditText

internal class SDLInputConnection(targetView: View?, fullEditor: Boolean) :
    BaseInputConnection(targetView!!, fullEditor) {
    protected var mEditText: EditText
    protected var mCommittedText = ""

    init {
        mEditText = EditText(SDL.getContext())
    }

    override fun getEditable(): Editable {
        return mEditText.editableText
    }

    override fun sendKeyEvent(event: KeyEvent): Boolean {
        /*
         * This used to handle the keycodes from soft keyboard (and IME-translated input from hardkeyboard)
         * However, as of Ice Cream Sandwich and later, almost all soft keyboard doesn't generate key presses
         * and so we need to generate them ourselves in commitText.  To avoid duplicates on the handful of keys
         * that still do, we empty this out.
         */

        /*
         * Return DOES still generate a key event, however.  So rather than using it as the 'click a button' key
         * as we do with physical keyboards, let's just use it to hide the keyboard.
         */
        if (event.keyCode == KeyEvent.KEYCODE_ENTER) {
            if (SDLActivity.onNativeSoftReturnKey()) {
                return true
            }
        }
        return super.sendKeyEvent(event)
    }

    override fun commitText(text: CharSequence, newCursorPosition: Int): Boolean {
        if (!super.commitText(text, newCursorPosition)) {
            return false
        }
        updateText()
        return true
    }

    override fun setComposingText(text: CharSequence, newCursorPosition: Int): Boolean {
        if (!super.setComposingText(text, newCursorPosition)) {
            return false
        }
        updateText()
        return true
    }

    override fun deleteSurroundingText(beforeLength: Int, afterLength: Int): Boolean {
        if (!super.deleteSurroundingText(beforeLength, afterLength)) {
            return false
        }
        updateText()
        return true
    }

    protected fun updateText() {
        val content = editable ?: return
        val text = content.toString()
        val compareLength = Math.min(text.length, mCommittedText.length)
        var matchLength: Int
        var offset: Int

        /* Backspace over characters that are no longer in the string */matchLength = 0
        while (matchLength < compareLength) {
            val codePoint = mCommittedText.codePointAt(matchLength)
            if (codePoint != text.codePointAt(matchLength)) {
                break
            }
            matchLength += Character.charCount(codePoint)
        }
        /* FIXME: This doesn't handle graphemes, like '🌬️' */offset = matchLength
        while (offset < mCommittedText.length) {
            val codePoint = mCommittedText.codePointAt(offset)
            nativeGenerateScancodeForUnichar('\b')
            offset += Character.charCount(codePoint)
        }
        if (matchLength < text.length) {
            val pendingText = text.subSequence(matchLength, text.length).toString()
            offset = 0
            while (offset < pendingText.length) {
                val codePoint = pendingText.codePointAt(offset)
                if (codePoint == '\n'.code) {
                    if (SDLActivity.onNativeSoftReturnKey()) {
                        return
                    }
                }
                /* Higher code points don't generate simulated scancodes */if (codePoint < 128) {
                    nativeGenerateScancodeForUnichar(codePoint.toChar())
                }
                offset += Character.charCount(codePoint)
            }
            nativeCommitText(pendingText, 0)
        }
        mCommittedText = text
    }

    companion object {
        @JvmStatic
        external fun nativeCommitText(text: String?, newCursorPosition: Int)
        @JvmStatic
        external fun nativeGenerateScancodeForUnichar(c: Char)
    }
}
