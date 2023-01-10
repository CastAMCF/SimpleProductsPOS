package com.ipt.simpleproductspos

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.appcompat.app.AppCompatActivity

class Product(id: Int, icon: String, name: String, quantity: Int, price: Double) {
	private var id = 0
	private var icon: String = ""
	private var name: String = ""
	private var quantity = 0
	private var price = 0.0

	init {
		this.id = id
		this.icon = icon
		this.name = name
		this.quantity = quantity
		this.price = price
	}

	fun getID(): Int {
		return id
	}

	fun getIcon(): String {
		return icon
	}

	fun getName(): String {
		return name
	}

	fun getQuantity(): Int {
		return quantity
	}

	fun getPrice(): Double {
		return price
	}

	fun getBitmap(context: Context): Bitmap? {
		val shre: SharedPreferences = context.getSharedPreferences("images", AppCompatActivity.MODE_PRIVATE)
		val previouslyEncodedImage: String? = shre.getString(icon, "")

		val byte = Base64.decode(previouslyEncodedImage, Base64.DEFAULT)

		return BitmapFactory.decodeByteArray(byte, 0, byte.size)
	}

	fun setIcon(icon: String) {
		this.icon = icon
	}

	fun setName(name: String) {
		this.name = name
	}

	fun setQuantity(quantity: Int) {
		this.quantity = quantity
	}

	fun setPrice(price: Double) {
		this.price = price
	}
}