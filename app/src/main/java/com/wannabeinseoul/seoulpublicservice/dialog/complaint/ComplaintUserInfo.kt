package com.wannabeinseoul.seoulpublicservice.dialog.complaint

data class ComplaintUserInfo(
    val svcId: String,
    val complaintId: String,
    val complaintName: String,
    val userId: String
) {
    companion object {
        fun newData() = ComplaintUserInfo("", "", "", "")
    }
}