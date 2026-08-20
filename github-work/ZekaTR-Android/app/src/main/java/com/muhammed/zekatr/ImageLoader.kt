package com.muhammed.zekatr

import android.widget.ImageView
import com.bumptech.glide.Glide

object ImageLoader {
    fun load(view: ImageView, url: String?) {
        if (url.isNullOrBlank()) return
        Glide.with(view.context).load(url).centerCrop().into(view)
    }
}
