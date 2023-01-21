package com.ipt.simpleproductspos.data

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.appcompat.app.AppCompatActivity

data class Product(var id: Int, var icon: String, var name: String, var quantity: Int, var price: Double) {

	fun getBitmap(context: Context): Bitmap? {
		val shre: SharedPreferences = context.getSharedPreferences("images", AppCompatActivity.MODE_PRIVATE)
		val previouslyEncodedImage: String? = shre.getString(icon, "")

		val byte = Base64.decode(previouslyEncodedImage, Base64.DEFAULT)

		return BitmapFactory.decodeByteArray(byte, 0, byte.size)
	}

}