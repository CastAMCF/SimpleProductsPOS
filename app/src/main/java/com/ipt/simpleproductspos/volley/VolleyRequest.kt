package com.ipt.simpleproductspos.volley

import android.content.Context
import com.android.volley.Request.Method
import com.android.volley.Response
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.ipt.simpleproductspos.volley.service.AuthService
import com.ipt.simpleproductspos.volley.service.ProductService
import com.ipt.simpleproductspos.volley.service.UserService
import org.json.JSONArray
import org.json.JSONObject


class VolleyRequest() {

	//vars para conectar ao api
	private val api = "https://nodeapidam-production.up.railway.app"

	private val apiLoginUrl = "${api}/auth"
	fun Auth(): AuthService = AuthService(apiLoginUrl)

	private val apiProductsUrl = "${api}/products"
	fun Product(): ProductService = ProductService(apiProductsUrl)

	private val apiUsersUrl = "${api}/users"
	fun User(): UserService = UserService(apiUsersUrl)



	fun newStringRequest(context: Context, url: String, method: Int, response: Response.Listener<String>, responseError: Response.ErrorListener,
			jsonBody: JSONObject? = null) {

		val jsonObjectRequest: StringRequest

		if(jsonBody == null){
			jsonObjectRequest = StringRequest(
				method, url, response, responseError
			)
		}else{
			jsonObjectRequest = object : StringRequest(
				method, url, response, responseError
			) {
				override fun getBodyContentType(): String {
					return "application/json; charset=utf-8"
				}
				//carregar as variaveis que pretendemos enviar para o API no body do pedido
				override fun getBody(): ByteArray {
					return jsonBody.toString().toByteArray()
				}

			}
		}

		//Adicionar o pedido à fila
		Volley.newRequestQueue(context).add(jsonObjectRequest)
	}

	fun newJsonArrayRequest(context: Context, url: String, response: Response.Listener<JSONArray>, responseError: Response.ErrorListener) {

		val jsonObjectRequest = JsonArrayRequest(Method.GET, url, null, response, responseError)

		//Adicionar o pedido à fila
		Volley.newRequestQueue(context).add(jsonObjectRequest)
	}
}