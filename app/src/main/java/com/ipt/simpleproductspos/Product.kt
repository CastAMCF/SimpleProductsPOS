package com.ipt.simpleproductspos

class Product(iconID: Int, name: String, quantity: Int, price: Double) {
	private var iconID = 0
	private var name: String = ""
	private var quantity = 0
	private var price = 0.0

	init {
		this.iconID = iconID
		this.name = name
		this.quantity = quantity
		this.price = price
	}

	fun getIconID(): Int {
		return iconID
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
}