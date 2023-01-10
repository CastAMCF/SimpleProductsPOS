package com.ipt.simpleproductspos

class User(id: Int, username: String, password: String, role: String) : java.io.Serializable {
    private var id: Int = 0
    private var username: String = ""
    private var role: String = ""
    private var password: String = ""

    init {
        this.id = id
        this.username = username
        this.role = role
        this.password = password
    }

    fun getID(): Int {
        return id
    }

    fun getUsername(): String {
        return username
    }

    fun getPassword(): String {
        return password;
    }

    fun getRole(): String {
        return role
    }

    fun setRole(role: String) {
        this.role = role
    }

}