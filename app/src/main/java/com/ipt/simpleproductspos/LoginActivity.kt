package com.ipt.simpleproductspos

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.ipt.simpleproductspos.databinding.ActivityLoginBinding
import org.json.JSONObject


class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    val apiLoginUrl = "https://nodeapidam-production.up.railway.app/auth"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val loginButton = binding.submitButton
        val infoButton = binding.infoButton
        val username = binding.nameInput
        val password = binding.passInput

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

        infoButton.setOnClickListener{
            val intent = Intent(this, AboutUsActivity::class.java)
            startActivity(intent)
            finish()
        }

    }
}