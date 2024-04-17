package com.wannabeinseoul.seoulpublicservice.ui.mypage

import android.graphics.drawable.Drawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.wannabeinseoul.seoulpublicservice.databinding.MyPageItemProfileBinding
import com.wannabeinseoul.seoulpublicservice.databinding.MyPageItemReviewedBinding
import com.wannabeinseoul.seoulpublicservice.databinding.MyPageItemReviewedHeaderBinding
import com.wannabeinseoul.seoulpublicservice.databinding.MyPageItemReviewedNothingBinding
import com.wannabeinseoul.seoulpublicservice.databinding.MyPageItemSavedBinding
import com.wannabeinseoul.seoulpublicservice.ui.recommendation.RecommendationData
import com.wannabeinseoul.seoulpublicservice.util.fromHtml
import com.wannabeinseoul.seoulpublicservice.util.loadWithHolder

private const val JJTAG = "jj-MyPageAdapter"

class MyPageAdapter(
    private val lifecycleOwner: LifecycleOwner,
    private val onClearClick: () -> Unit,
    private val onReviewedClick: (svcid: String) -> Unit,
    private val onSettingClick: () -> Unit,
) : ListAdapter<MyPageAdapter.MultiView, MyPageAdapter.CommonViewHolder>(
    object : DiffUtil.ItemCallback<MultiView>() {
        override fun areItemsTheSame(oldItem: MultiView, newItem: MultiView): Boolean =
            oldItem === newItem

        override fun areContentsTheSame(oldItem: MultiView, newItem: MultiView): Boolean =
            oldItem == newItem
    }
) {

//    /** 프래그먼트에서 멀티뷰 아이템의 isVisible을 변경하기 위한 람다식 */
//    /** 라이브데이터 직접 넘겨주면서 안씀 */
//    var setSavedNothingVisible: ((boolean: Boolean) -> Unit)? = null

    /** 멀티뷰 sealed interface */
    sealed interface MultiView {

        enum class Type {
            PROFILE,
            SAVED,
            REVIEWED_HEADER,
            REVIEWED,
            REVIEWED_NOTHING,
//            LOADING,
        }

        val viewType: Type

        data class Profile(
//            val userId: String?,
            val userColor: Int,
            val userDrawable: LiveData<Drawable?>,
            val userName: LiveData<String?>,
            val onEditButtonClick: () -> Unit,
        ) : MultiView {
            override val viewType: Type = Type.PROFILE
        }

        data class Saved(
            val myPageSavedAdapter: MyPageSavedAdapter,
            val savedList: LiveData<List<RecommendationData?>>,
        ) : MultiView {
            override val viewType: Type = Type.SAVED
        }

        data object ReviewedHeader : MultiView {
            override val viewType: Type = Type.REVIEWED_HEADER
        }

        data class Reviewed(
            val reviewedData: ReviewedData
        ) : MultiView {
            override val viewType: Type = Type.REVIEWED
        }

        data object ReviewedNothing : MultiView {
            override val viewType: Type = Type.REVIEWED_NOTHING
        }

//        data object Loading : MultiView {
//            override val viewType: Type = Type.LOADING
//        }

    }


    /** 뷰홀더들 */
    abstract inner class CommonViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        abstract fun onBind(item: MultiView)
    }

    inner class ProfileHolder(private val b: MyPageItemProfileBinding) :
        CommonViewHolder(b.root) {

        private var isNotInitialized = true

        override fun onBind(item: MultiView) {
            if (isNotInitialized) {
                (item as MultiView.Profile)
                b.clProfileEdit.setOnClickListener {
                    item.onEditButtonClick()
                }

                b.ivProfileSetting.setOnClickListener {
                    onSettingClick()
                }

                b.ivProfileProfile.drawable.setTint(item.userColor)
                item.userName.observe(lifecycleOwner) { b.tvProfileNickname.text = it }
                item.userDrawable.observe(lifecycleOwner) { it?.let { b.ivProfileProfile.load(it) } }
                isNotInitialized = false
            }
        }
    }

    inner class SavedHolder(private val b: MyPageItemSavedBinding) :
        CommonViewHolder(b.root) {

        init {
            b.tvSavedClear.setOnClickListener { onClearClick() }
        }

        private var isAdapterNotBound = true

        override fun onBind(item: MultiView) {
            item as MultiView.Saved
            if (isAdapterNotBound) {
                b.rvSaved.adapter = item.myPageSavedAdapter
                isAdapterNotBound = false
                item.savedList.observe(lifecycleOwner) {
                    Log.d(JJTAG, "옵저버:savedList ${it.toString().take(255)}")
                    item.myPageSavedAdapter.submitList(it) {
                        b.tvSavedNothing.isVisible = it.isEmpty()
                    }
                }
            }
        }
    }

    inner class ReviewedHeaderHolder(b: MyPageItemReviewedHeaderBinding) :
        CommonViewHolder(b.root) {
        override fun onBind(item: MultiView) {}
    }

    inner class ReviewedHolder(private val b: MyPageItemReviewedBinding) :
        CommonViewHolder(b.root) {

        override fun onBind(item: MultiView) {
            val reviewedData = (item as MultiView.Reviewed).reviewedData
            val entity = item.reviewedData.entity
            b.ivReviewedThumbnail.loadWithHolder(entity.IMGURL)
            b.tvReviewedArea.text = entity.AREANM
            b.tvReviewedTitle.text = entity.SVCNM.fromHtml()
            b.tvReviewedReviewContent.text = reviewedData.content
            b.tvReviewedDate.text = reviewedData.uploadTime.let {
                if (it.length > 15) it.substring(2..15)
                else it
            }

            b.root.setOnClickListener { onReviewedClick(entity.SVCID) }
        }
    }

    inner class ReviewedNothingHolder(b: MyPageItemReviewedNothingBinding) :
        CommonViewHolder(b.root) {
        override fun onBind(item: MultiView) {}
    }

//    inner class LoadingHolder(private val b: ItemLoadingProgressBinding) :
//        CommonViewHolder(b.root) {
//        override fun onBind(item: MultiView) {
////            Log.d(JJTAG, "LoadingHolder onBind ${b.root}")
//            b.root.isVisible = itemCount > 4
//        }
//    }

    /* 뷰홀더 끝 */


    override fun getItemViewType(position: Int): Int = getItem(position).viewType.ordinal

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommonViewHolder {
        return when (MultiView.Type.values()[viewType]) {
            MultiView.Type.PROFILE -> ProfileHolder(
                MyPageItemProfileBinding
                    .inflate(LayoutInflater.from(parent.context), parent, false)
            )

            MultiView.Type.SAVED -> SavedHolder(
                MyPageItemSavedBinding
                    .inflate(LayoutInflater.from(parent.context), parent, false)
            )

            MultiView.Type.REVIEWED_HEADER -> ReviewedHeaderHolder(
                MyPageItemReviewedHeaderBinding
                    .inflate(LayoutInflater.from(parent.context), parent, false)
            )

            MultiView.Type.REVIEWED -> ReviewedHolder(
                MyPageItemReviewedBinding
                    .inflate(LayoutInflater.from(parent.context), parent, false)
            )

            MultiView.Type.REVIEWED_NOTHING -> ReviewedNothingHolder(
                MyPageItemReviewedNothingBinding
                    .inflate(LayoutInflater.from(parent.context), parent, false)
            )

//            MultiView.Type.LOADING -> LoadingHolder(
//                ItemLoadingProgressBinding
//                    .inflate(LayoutInflater.from(parent.context), parent, false)
//            )
        }
    }

    override fun onBindViewHolder(holder: CommonViewHolder, position: Int) {
        holder.onBind(getItem(position))
    }
}
