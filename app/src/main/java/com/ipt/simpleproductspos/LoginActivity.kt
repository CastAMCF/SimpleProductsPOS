package com.ipt.simpleproductspos

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.Toast
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

class LoginActivity : AppCompatActivity() {

    val apiLoginUrl = "https://nodeapidam-production.up.railway.app/auth"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val loginButton = findViewById<View>(R.id.submit_button)
        val username = findViewById<View>(R.id.nameInput) as EditText
        val password = findViewById<View>(R.id.passInput) as EditText

        loginButton.setOnClickListener{
            val usernameTxt = username.text.toString()
            val passwordTxt = password.text.toString()

            if (usernameTxt.isNotEmpty()) {
                if (passwordTxt.isNotEmpty()) {

                    val jsonObjectRequest = object : StringRequest(
                        Method.POST, apiLoginUrl,
                        { response ->

                            val res = response.trim('"').split(';')

                            if (response.contains("employee") || response.contains("manager")){
                                val session = User(res[0].toInt(), usernameTxt, passwordTxt, res[1])
                                val intent = Intent(this, MainActivity::class.java)
                                intent.putExtra("sessao", session)
                                finish()
                                startActivity(intent)
                            } else {
                                Toast.makeText(this, "O nome de utilizador ou palavra-passe está incorreto", Toast.LENGTH_SHORT).show()
                            }
                        },
                        { error ->
                            Log.e("res", error.toString())
                            Toast.makeText(this, "Não foi possível fazer o login, tente novamente mais tarde", Toast.LENGTH_SHORT).show()

                        }
                    ) {
                        override fun getBodyContentType(): String {
                            return "application/json; charset=utf-8"
                        }

                        override fun getBody(): ByteArray {
                            val jsonBody = JSONObject()
                            jsonBody.put("username", usernameTxt)
                            jsonBody.put("password", passwordTxt)
                            return jsonBody.toString().toByteArray()
                        }


                    }

                    Volley.newRequestQueue(this).add(jsonObjectRequest)


                }else{
                    username.error = "A Palavra-Passe é obrigatória"
                }

            }else{
                password.error = "O Nome de Utilizador é obrigatório"
            }

        }

    }
}