package com.ipt.simpleproductspos.volley.service

import android.content.Context
import com.android.volley.Request
import com.android.volley.Response
import com.ipt.simpleproductspos.volley.VolleyRequest
import org.json.JSONObject

class AuthService(private val api: String) {

    fun login(context: Context, response: Response.Listener<String>, responseError: Response.ErrorListener, jsonBody: JSONObject
    ) =
        VolleyRequest().newStringRequest(context, api, Request.Method.POST, response, responseError, jsonBody)

}