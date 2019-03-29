package com.mvproject.updater

data class Update(
    val latestVersion : String,
    val file : String,
    val url : String)