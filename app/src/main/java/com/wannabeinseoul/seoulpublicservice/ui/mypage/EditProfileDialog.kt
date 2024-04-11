package com.wannabeinseoul.seoulpublicservice.ui.mypage

import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.inputmethod.InputMethodManager
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import coil.load
import com.wannabeinseoul.seoulpublicservice.R
import com.wannabeinseoul.seoulpublicservice.SeoulPublicServiceApplication
import com.wannabeinseoul.seoulpublicservice.databinding.CustomProgressBinding
import com.wannabeinseoul.seoulpublicservice.databinding.DialogEditProfileBinding
import com.wannabeinseoul.seoulpublicservice.ui.main.MainViewModel
import com.wannabeinseoul.seoulpublicservice.util.toastShort
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val JJTAG = "jj-에딧프로필"

class EditProfileDialog : DialogFragment() {

    companion object {
        fun newInstance() = EditProfileDialog()
    }

    private var _binding: DialogEditProfileBinding? = null
    private val binding get() = _binding!!

    private val app by lazy { requireActivity().application as SeoulPublicServiceApplication }
    private val container by lazy { app.container }
    private val id by lazy { container.idPrefRepository.load() }

    private val mainViewModel: MainViewModel by activityViewModels()

    private val imm by lazy {
        requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    }

    private var currentUri: Uri? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogEditProfileBinding.inflate(inflater, container, false)

        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.window?.requestFeature(Window.FEATURE_NO_TITLE)
        binding.ivEditProfileImage.load(app.userProfileImageDrawable.value) {
            error(app.userProfileImagePlaceholder)
        }
        binding.etEditProfileName.setText(app.userName.value)

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setListeners()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setListeners() = binding.let { b ->
        b.btnEditProfileCancel.setOnClickListener { dismiss() }

        b.btnEditProfileOkay.setOnClickListener {
            val name = b.etEditProfileName.text.toString()
            val drawable = b.ivEditProfileImage.drawable
            val isNewName = app.userName.value != name
            val isNewImage = currentUri != null && app.userProfileImageDrawable.value != drawable

            if (isNewName) {
                lifecycleScope.launch(Dispatchers.IO) {
                    val progressDialog = withContext(Dispatchers.Main) {
                        showProgressDialog()
                    }
                    val isDuplicated = false

                    progressDialog.dismiss()
                    if (isDuplicated) {
                        withContext(Dispatchers.Main) {
                            showDuplicatedDialog()
                        }
                        return@launch
                    }

                    app.userName.postValue(name)
                    mainViewModel.setUserName(name)
                    app.container.namePrefRepository.save(name)

                    if (isNewImage) uploadProfileImage(drawable)

                    dismiss()
                }
            } else {
                if (isNewImage) uploadProfileImage(drawable)
                dismiss()
            }
        }

        b.ivEditProfileImage.setOnClickListener {
            openGalleryWithPermissionCheck()
        }

        b.etEditProfileName.addTextChangedListener { editable: Editable? ->
            val text = editable?.toString() ?: ""
            val regex = Regex("^[a-zA-Z가-힣0-9]+$")
            b.etEditProfileName.error = when {
                text.length < 2 -> "닉네임은 2자 이상이어야 합니다"
                text.length > 10 -> "닉네임은 10자 이하여야 합니다"
                text == app.userName.value -> null  // 초기 익명-123456 그대로 하기 위한 조건
                regex.matches(text).not() -> "닉네임은 영문 대소문자, 한글, 숫자만 가능합니다"
                else -> null
            }
            b.btnEditProfileOkay.isEnabled = b.etEditProfileName.error == null
        }

        b.root.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                v.clearFocus()
                imm.hideSoftInputFromWindow(v.windowToken, 0)
            }
            true
        }
    }

    private fun uploadProfileImage(newDrawable: Drawable): Job {
        return CoroutineScope(Dispatchers.IO).launch {
            // 안전장치가 필요할 것 같다
            app.userProfileImageDrawable.postValue(newDrawable)
        }
    }

    private val requestPermissionLauncher: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) openGalleryPermitted()
            else Log.w(JJTAG, "콜백:requestPermissionLauncher not granted")
        }

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != RESULT_OK) {
                Log.e(JJTAG, "콜백:pickImageLauncher resultCode != RESULT_OK, result: $result")
                return@registerForActivityResult
            }
            currentUri = result.data?.data ?: return@registerForActivityResult Unit.apply {
                Log.e(JJTAG, "콜백:pickImageLauncher result.data: ${result.data}")
            }
            Log.d(JJTAG, "콜백:pickImageLauncher uri: $currentUri")

            binding.ivEditProfileImage.load(currentUri)
        }

    private fun openGalleryPermitted() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI)
        pickImageLauncher.launch(intent)
    }

    private fun openGalleryWithPermissionCheck() {
        // TODO: 한번 권한 거부하면 다시 요청이 불가능한 문제. 버튼이 무반응이 된다. 해결 불가능한가..? 일단 토스트로 반응.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = android.Manifest.permission.READ_MEDIA_IMAGES
            if (ContextCompat.checkSelfPermission(requireContext(), permission)
                != PackageManager.PERMISSION_GRANTED
            ) {
                toastShort(requireContext(), "미디어 이미지 권한이 필요합니다")
                requestPermissionLauncher.launch(permission)
            } else openGalleryPermitted()
        } else {
            val permission = android.Manifest.permission.READ_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(requireContext(), permission)
                != PackageManager.PERMISSION_GRANTED
            ) {
                toastShort(requireContext(), "외부 저장소 권한이 필요합니다")
                requestPermissionLauncher.launch(permission)
            } else openGalleryPermitted()
        }
    }

    private fun showDuplicatedDialog() = AlertDialog.Builder(requireContext()).apply {
        setTitle("중복된 닉네임입니다")
        setIcon(R.mipmap.ic_launcher)
        setNeutralButton("닫기", null)
    }.show()

    private fun showProgressDialog() = AlertDialog.Builder(requireContext()).apply {
        setTitle("서버와 통신하고 있습니다")
        setIcon(R.mipmap.ic_launcher)
        setView(CustomProgressBinding.inflate(layoutInflater).root)
        setCancelable(false)
    }.show()

}
