package com.ipt.simpleproductspos.data

private lateinit var session: User

private var myUsers: ArrayList<User> = arrayListOf()
private var myProducts: ArrayList<Product> = arrayListOf()
private var totalPrice: Double = 0.0

class Session() {

    fun getUser(): User {
        return session
    }

    fun setUser(id: Int, username: String, password: String, role: String) {
        session = User(id, username, password, role)
    }

    fun getUsers(): ArrayList<User> {
        return myUsers
    }

    fun getProducts(): ArrayList<Product> {
        return myProducts
    }

    fun getTotalPrice(): Double {
        return totalPrice
    }

    fun setTotalPrice(price: Double) {
        totalPrice = price
    }

    fun subTotalPrice(price: Double) {
        totalPrice -= price
    }

    fun addTotalPrice(price: Double) {
        totalPrice += price
    }

}