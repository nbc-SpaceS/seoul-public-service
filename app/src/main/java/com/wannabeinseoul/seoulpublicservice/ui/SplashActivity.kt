package com.wannabeinseoul.seoulpublicservice.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.animation.AlphaAnimation
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.wannabeinseoul.seoulpublicservice.SeoulPublicServiceApplication
import com.wannabeinseoul.seoulpublicservice.databases.entity.UserEntity
import com.wannabeinseoul.seoulpublicservice.databinding.ActivitySplashBinding
import com.wannabeinseoul.seoulpublicservice.ui.main.MainActivity
import com.wannabeinseoul.seoulpublicservice.util.DLog
import com.wannabeinseoul.seoulpublicservice.util.toastLong
import java.util.UUID

private const val JJTAG = "jj-SplashActivity"

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private val binding: ActivitySplashBinding by lazy {
        ActivitySplashBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        setContentView(binding.root)

        var initialLoadingFinished = false
        var createFinished = false

        /** 액티비티에서 의존성 주입 */
        val app = (application as SeoulPublicServiceApplication)
        val container = app.container

        binding.tvSplashDescription.startAnimation(AlphaAnimation(0.0f, 1.0f).apply {
            startOffset = 100
            duration = 1500
        })

        app.initialLoadingFinished.let { livedata ->
            livedata.observe(this) {
                DLog.d("jj-스플래시", "옵저버:initialLoadingFinished - $it")
                if (it != true) return@observe
                if (container.dbMemoryRepository.getAll().isEmpty()) {
                    DLog.w(JJTAG, "obs:app.initialLoadingFinished dbMemoryRepository.getAll empty")
                    toastLong(this, "네트워크 통신이 불가능하여 표시할 데이터가 없습니다. 앱을 종료합니다.")
                    finishAffinity()
                    return@observe
                }
                if (createFinished) {
                    DLog.d("jj-스플래시", "옵저버에서 이동 (스플래시 create가 먼저 끝남, 일반적)")
                    moveToNextActivity()
                    return@observe
                }
                initialLoadingFinished = it
            }
            initialLoadingFinished = livedata.value!!
        }

        val loadedId = container.idPrefRepository.load()
        if (loadedId.isBlank()) {
            val id = UUID.randomUUID().toString()
            val user = UserEntity(
                userId = id,
                userName = "익명-${id.substring(0..5)}",
                userProfileImage = "",
                userColor = "#" + (1..6).map { id.replace("-", "").random() }.joinToString(""),
                reviewIdList = emptyList()
            )
            container.idPrefRepository.save(id)
            app.setUser(user)
        }

        container.filterPrefRepository.clearData()
        container.savedPrefRepository.setFlag(false)

//        Handler(Looper.getMainLooper()).postDelayed(Runnable {
//            startActivity(Intent(this@SplashActivity, MainActivity::class.java))
//            finish()
//        }, 500)

        if (initialLoadingFinished) {
            DLog.d("jj-스플래시", "onCreate에서 이동 (메인 리스트 로딩이 먼저 끝남, 특이 케이스)")
            moveToNextActivity()
        } else createFinished = true
    }

    private fun moveToNextActivity() {
        startActivity(Intent(this@SplashActivity, MainActivity::class.java))
        finish()
    }
}