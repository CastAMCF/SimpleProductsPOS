package com.ipt.simpleproductspos.volley.service

import android.content.Context
import com.android.volley.Request
import com.android.volley.Response
import com.ipt.simpleproductspos.data.Session
import com.ipt.simpleproductspos.volley.VolleyRequest
import org.json.JSONArray
import org.json.JSONObject

class UserService(private val api: String)  {

    fun getAll(context: Context, response: Response.Listener<JSONArray>, responseError: Response.ErrorListener
    ) =
        VolleyRequest().newJsonArrayRequest(context, "${api}?username=${Session().getUser().username}&password=${Session().getUser().password}", response, responseError)


    fun create(context: Context, response: Response.Listener<String>, responseError: Response.ErrorListener, jsonBody: JSONObject
    ) =
        VolleyRequest().newStringRequest(context, api, Request.Method.POST, response, responseError, jsonBody)


    fun update(context: Context, username: String, response: Response.Listener<String>, responseError: Response.ErrorListener, jsonBody: JSONObject
    ) =
        VolleyRequest().newStringRequest(context, "${api}/${username}", Request.Method.PUT, response, responseError, jsonBody)


    fun delete(context: Context, username: String, response: Response.Listener<String>, responseError: Response.ErrorListener
    ) =
        VolleyRequest().newStringRequest(context, "${api}/${username}", Request.Method.DELETE, response, responseError)

}