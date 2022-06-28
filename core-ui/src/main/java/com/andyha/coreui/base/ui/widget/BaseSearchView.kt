package com.andyha.coreui.base.ui.widget

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Build
import android.speech.RecognizerIntent
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.inputmethod.EditorInfo
import android.widget.SearchView
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.andyha.coreextension.expandClickArea
import com.andyha.coreextension.hideKeyboard
import com.andyha.coreextension.showKeyboard
import com.andyha.coreui.R
import com.andyha.coreui.databinding.BaseSearchViewInternalBinding


class BaseSearchView : ConstraintLayout {

    private var mQueryHint: String? = null
    private var mTextHintColor: Int = 0
    private var mEnableClearIcon: Boolean = true
    private var mClearIcon: Drawable? = null
    private var mTextCursorDrawable: Drawable? = null
    private var mEnableVoiceSearch: Boolean = true
    private var mVoiceSearchIcon: Drawable? = null
    private var mSearchIcon: Drawable? = null
    private var mInputTextAppearance: Int = 0
    private var mEnableMatchedResult: Boolean = false
    private var mEnableClick = false

    private var mVoiceSearchResultLauncher: ActivityResultLauncher<Unit>? = null

    private var mOnQueryTextListener: SearchView.OnQueryTextListener? = null
    private var mOnClickListener: (() -> Unit)? = null
    private var mOnFocusChangeListener: ((Boolean) -> Unit)? = null
    private var mOnClickClearIconListener: (() -> Unit)? = null
    private var mOnClickSearchIconListener: (() -> Unit)? = null
    private var mOnSubmitSearchIncludeEmpty: ((String?) -> Unit)? = null

    private var mCurrentFragment: Fragment? = null

    private lateinit var viewBinding: BaseSearchViewInternalBinding

    constructor(context: Context, attrs: AttributeSet?) : this(
        context,
        attrs,
        R.attr.BaseSearchViewStyle
    )

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : this(
        context,
        attrs,
        defStyleAttr,
        R.style.BaseSearchViewStyle
    )

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes) {
        getAttributes(context, attrs, defStyleAttr, defStyleRes)
        initView(context)
    }

    private fun getAttributes(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) {
        val typeArray = context.obtainStyledAttributes(
            attrs,
            R.styleable.BaseSearchView,
            defStyleAttr,
            defStyleRes
        )

        mQueryHint = typeArray.getString(R.styleable.BaseSearchView_BS_queryHint)
        mEnableClearIcon =
            typeArray.getBoolean(R.styleable.BaseSearchView_BS_enableClearIcon, mEnableClearIcon)
        mClearIcon = typeArray.getDrawable(R.styleable.BaseSearchView_BS_clearIcon)
        mTextCursorDrawable = typeArray.getDrawable(R.styleable.BaseSearchView_BS_textCursorDrawable)
        mEnableVoiceSearch =
            typeArray.getBoolean(R.styleable.BaseSearchView_BS_enableVoiceSearch, mEnableVoiceSearch)
        mVoiceSearchIcon = typeArray.getDrawable(R.styleable.BaseSearchView_BS_voiceSearchIcon)
        mSearchIcon = typeArray.getDrawable(R.styleable.BaseSearchView_BS_searchIcon)
        mEnableMatchedResult = typeArray.getBoolean(
            R.styleable.BaseSearchView_BS_enableMatchedResult,
            mEnableMatchedResult
        )
        mTextHintColor =
            typeArray.getResourceId(R.styleable.BaseSearchView_BS_textHintColor, mTextHintColor)
        mInputTextAppearance = typeArray.getResourceId(
            R.styleable.BaseSearchView_BS_inputTextAppearance,
            mInputTextAppearance
        )
        mEnableClick = typeArray.getBoolean(R.styleable.BaseSearchView_BS_enableClick, mEnableClick)

        typeArray.recycle()
    }

    private fun initView(context: Context) {
        setBackgroundResource(R.drawable.round_white_edit_text)

        viewBinding = BaseSearchViewInternalBinding.inflate(LayoutInflater.from(context), this)

        viewBinding.icSearchLeft.apply {
            isVisible = mSearchIcon != null
            setImageDrawable(mSearchIcon)
            expandClickArea()
            setOnClickListener {
                if (mOnClickSearchIconListener != null) {
                    mOnClickSearchIconListener?.invoke()
                } else {
                    showKeyboard()
                    if (!viewBinding.inputText.text.isNullOrEmpty()) {
                        viewBinding.inputText.setSelection(viewBinding.inputText.text.length)
                    }
                }
            }
        }

        viewBinding.inputText.apply {
            hint = mQueryHint
            setTextAppearance(mInputTextAppearance)
            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == KeyEvent.KEYCODE_BACK) {
                    removeFocus()
                    val content = viewBinding.inputText.text.toString().trimStart()
                    if (content.isNotEmpty()) {
                        mOnQueryTextListener?.onQueryTextSubmit(content)
                    }
                    mOnSubmitSearchIncludeEmpty?.invoke(viewBinding.inputText.text.toString())
                }
                false
            }
            addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(query: Editable?) {
                    //update icon of right button
                    if (query.isNullOrEmpty()) {
                        viewBinding.rightButton.isVisible = mEnableVoiceSearch
                        viewBinding.rightButton.setImageDrawable(mVoiceSearchIcon)
                    } else {
                        viewBinding.rightButton.isVisible = mEnableClearIcon
                        viewBinding.rightButton.setImageDrawable(mClearIcon)
                    }

                    mOnQueryTextListener?.onQueryTextChange(query?.trim().toString())
                }

                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                    //do nothing
                }

                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                    //do nothing
                }
            })
            setOnFocusChangeListener { _, isFocus ->
                if (isFocus) {
                    viewBinding.inputText.showKeyboard()
                    mOnFocusChangeListener?.invoke(true)
                } else {
                    viewBinding.inputText.hideKeyboard()
                    mOnFocusChangeListener?.invoke(false)
                }
            }
            mTextCursorDrawable?.let { setCursorDrawable(it) }
        }

        if (mEnableVoiceSearch) {
            viewBinding.rightButton.setImageDrawable(mVoiceSearchIcon)
        } else {
            viewBinding.rightButton.isVisible = false
        }

        if (mEnableMatchedResult) {
            viewBinding.tvMatchedNo.isVisible = true
            viewBinding.tvMatchedNo.setTextAppearance(mInputTextAppearance)
        } else {
            viewBinding.tvMatchedNo.isVisible = false
        }

        if (mEnableClick) {
            viewBinding.clickView.isVisible = true
            viewBinding.clickView.setOnClickListener {
                mOnClickListener?.invoke()
            }
            viewBinding.inputText.isEnabled = false
            return
        } else {
            viewBinding.clickView.isVisible = false
        }

        viewBinding.rightButton.expandClickArea()
        viewBinding.rightButton.setOnClickListener {
            if (viewBinding.inputText.text.isEmpty() && mEnableVoiceSearch) {
                //Click voice search
                removeFocus()

                mVoiceSearchResultLauncher?.launch(Unit)
            } else {
                viewBinding.tvMatchedNo.setText("")
                viewBinding.inputText.setText("")
                showKeyboard()

                mOnClickClearIconListener?.invoke()
            }
        }
    }

    private fun showKeyboard() {
        setFocus()
        viewBinding.inputText.showKeyboard()
    }

    fun setCurrentFragment(fragment: Fragment?) {
        this.mCurrentFragment = fragment
    }

    fun setOnQueryTextListener(listener: SearchView.OnQueryTextListener) {
        this.mOnQueryTextListener = listener
    }

    fun setOnClickListener(listener: () -> Unit) {
        this.mOnClickListener = listener
    }

    fun setOnFocusChangedClickListener(listener: (Boolean) -> Unit) {
        this.mOnFocusChangeListener = listener
    }

    fun setOnClickClearIconListener(listener: () -> Unit) {
        this.mOnClickClearIconListener = listener
    }

    fun setOnClickSearchIconListener(listener: () -> Unit) {
        this.mOnClickSearchIconListener = listener
    }

    fun setVoiceSearchResultLauncher(voiceSearchResultLauncher: ActivityResultLauncher<Unit>) {
        this.mVoiceSearchResultLauncher = voiceSearchResultLauncher
    }

    fun setOnSubmitSearchIncludeEmpty(listener: (String?) -> Unit) {
        this.mOnSubmitSearchIncludeEmpty = listener
    }

    /**
     * Set content to edit text in search view and query search
     *
     * @param content
     * @param isSubmit
     */
    fun setQuery(content: String?, isSubmit: Boolean) {
        viewBinding.inputText.setText(content)

        if (mEnableClick) {
            return
        }
        if (isSubmit) {
            mOnQueryTextListener?.onQueryTextSubmit(content)
        }
    }

    fun getQuery(): String {
        return viewBinding.inputText.text.toString()
    }

    fun removeFocus() {
        if (mEnableClick) return
        viewBinding.inputText.hideKeyboard()
        clearFocus()
    }

    fun setFocus() {
        if (mEnableClick) return
        viewBinding.inputText.requestFocus()
    }

    fun clearText() {
        if (mEnableClick) return
        viewBinding.tvMatchedNo.setText("")
        viewBinding.inputText.setText("")
        removeFocus()
    }

    fun checkFocus(): Boolean {
        return viewBinding.inputText.isFocused
    }

    fun setQueryHint(textHint: Int) {
        viewBinding.inputText.setHint(textHint)
    }

    class VoiceSearchActivityResultContract : ActivityResultContract<Unit, String>() {
        override fun createIntent(context: Context, input: Unit?): Intent {
            //using recognize service of android to listen voice search
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            intent.putExtra(
                RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS,
                2000
            )
            intent.putExtra(
                RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS,
                2000
            )
            intent.putExtra(
                RecognizerIntent.EXTRA_PROMPT,
                context.getString(R.string.common_voice_search)
            )
            return intent
        }

        override fun parseResult(resultCode: Int, intent: Intent?): String {
            if (resultCode != Activity.RESULT_OK || intent == null) {
                return ""
            }

            //listen speech to text (Voice search)
            val matches: ArrayList<String> =
                intent.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS) ?: return ""
            return matches.first()
        }

    }

    private fun TextView.setCursorDrawable(drawable: Drawable) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            this.textCursorDrawable = drawable
            return
        } else {
            try {
                val editorField = try {
                    TextView::class.java.getDeclaredField("mEditor").apply { isAccessible = true }
                } catch (t: Throwable) {
                    null
                }
                val editor = editorField?.get(this) ?: this
                val editorClass: Class<*> =
                    if (editorField == null) TextView::class.java else editor.javaClass

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    editorClass
                        .getDeclaredField("mDrawableForCursor")
                        .apply { isAccessible = true }
                        .run { set(editor, drawable) }
                } else {
                    editorClass
                        .getDeclaredField("mCursorDrawable")
                        .apply { isAccessible = true }
                        .run { set(editor, arrayOf(drawable, drawable)) }
                }
            } catch (t: Throwable) {
                t.printStackTrace()
            }
        }
    }
}