package com.andyha.coreui.base.ui.widget.dialog

import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import androidx.annotation.StringRes
import androidx.fragment.app.FragmentManager
import com.andyha.coreextension.TAG
import com.andyha.coreextension.show
import com.andyha.coreui.base.fragment.BaseDialogFragment
import com.andyha.coreui.databinding.FragmentTimePickerDialogBinding
import java.util.*

class TimePickerDialogFragment : BaseDialogFragment<FragmentTimePickerDialogBinding>({
    FragmentTimePickerDialogBinding.inflate(it)
}) {
    companion object {
        fun showDialog(
            fm: FragmentManager,
            cancelable: Boolean = true,
            is24hFormat: Boolean = true,
            title: String? = null,
            hours: Int? = null,
            minutes: Int? = null,
            @StringRes titleResId: Int? = null,
            option1Text: String? = null,
            @StringRes option1ResId: Int? = null,
            option2Text: String? = null,
            @StringRes option2ResId: Int? = null,
            onOption1Clicked: (() -> Unit)? = null,
            onOption2Clicked: ((Int, Int) -> Unit)? = null,
            onDismissListener: (() -> Unit)? = null,
        ): TimePickerDialogFragment {
            val dialog = TimePickerDialogFragment()
            dialog.mCancelable = cancelable
            dialog.is24hFormat = is24hFormat
            dialog.hours = hours
            dialog.minutes = minutes

            dialog.title = title
            dialog.titleResId = titleResId

            dialog.option1Text = option1Text
            dialog.option1ResId = option1ResId

            dialog.option2Text = option2Text
            dialog.option2ResId = option2ResId

            dialog.onOption1Clicked = onOption1Clicked
            dialog.onOption2Clicked = onOption2Clicked
            dialog.onDismissListener = onDismissListener

            dialog.show(fm, TAG)
            return dialog
        }
    }

    var title: String? = null
    var titleResId: Int? = null
    var option1Text: String? = null
    var option1ResId: Int? = null
    var option2Text: String? = null
    var option2ResId: Int? = null
    var onOption1Clicked: (() -> Unit)? = null
    var onOption2Clicked: ((Int, Int) -> Unit)? = null
    var onDismissListener: (() -> Unit)? = null
    var mCancelable: Boolean = false
    var is24hFormat = true
    var hours: Int? = null
    var minutes: Int? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateView()
    }

    private fun updateView() {
        isCancelable = mCancelable

        title?.let {
            viewBinding.tvTitle.text = it
            viewBinding.tvTitle.show()
        }

        titleResId?.let {
            viewBinding.tvTitle.setText(it)
            viewBinding.tvTitle.show()
        }

        if (option1Text != null || option1ResId != null) {
            showOption1()
        }

        if (option2Text != null || option2ResId != null) {
            showOption2()
        }

        option1Text?.let {
            viewBinding.btnOption1.text = it
        }

        option1ResId?.let {
            viewBinding.btnOption1.setText(it)
        }

        option2Text?.let {
            viewBinding.btnOption2.text = it
        }

        option2ResId?.let {
            viewBinding.btnOption2.setText(it)
        }
        viewBinding.timePicker.setIs24HourView(is24hFormat)
        if (hours != null && minutes != null) {
            viewBinding.timePicker.hour = hours!!
            viewBinding.timePicker.minute = minutes!!
        } else {
            viewBinding.timePicker.hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            viewBinding.timePicker.minute = Calendar.getInstance().get(Calendar.MINUTE)
        }

        viewBinding.btnOption1.setOnClickListener { onOption1Clicked?.invoke();dismiss() }
        viewBinding.btnOption2.setOnClickListener {
            onOption2Clicked?.invoke(
                viewBinding.timePicker.hour,
                viewBinding.timePicker.minute
            );dismiss()
        }
    }

    private fun showOption2() {
        viewBinding.llOption.show()
        viewBinding.btnOption2.show()
    }

    private fun showOption1() {
        viewBinding.llOption.show()
        viewBinding.btnOption1.show()
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        onDismissListener?.invoke()
    }
}