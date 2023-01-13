package com.ipt.simpleproductspos.ui.main

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.ipt.simpleproductspos.MainActivity
import com.ipt.simpleproductspos.R
import com.ipt.simpleproductspos.User
import org.json.JSONObject

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [UsersFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class UsersFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private lateinit var mainActivity: MainActivity
    private lateinit var apiUsersUrl: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }

        mainActivity = (activity as MainActivity)
        apiUsersUrl = mainActivity.apiUsersUrl

        mainActivity.refreshUsersList()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view: View = inflater.inflate(R.layout.fragment_users, container, false)

        mainActivity.hideKeyboard()

        val list: ListView = view.findViewById(R.id.usersListView)
        val myListAdapter =  mainActivity.myUserListAdapter
        list.adapter = myListAdapter
        //atualizar a lista ao "puxar" para baixo
        val refreshLayout: SwipeRefreshLayout = view.findViewById(R.id.refreshLayout)
        refreshLayout.setOnRefreshListener {

            myListAdapter.notifyDataSetChanged()

            refreshLayout.isRefreshing = false
        }
        //listener ao pressionar em um user para o editar
        list.setOnItemClickListener { adapterView, view, i, l ->

            val selectUser: User = list.getItemAtPosition(i) as User
            editUserDialog(requireContext(), selectUser, myListAdapter)

        }
        //listener ao manter premido em um user para o remover
        list.setOnItemLongClickListener { adapterView, view, i, l ->

            val selectUser: User = list.getItemAtPosition(i) as User
            removeUserDialog(requireContext(), selectUser, myListAdapter)

            true
        }

        //botão flutuante para adicionar um novo user
        val addUser: FloatingActionButton = view.findViewById(R.id.add_user_card)

        addUser.setOnClickListener { view ->
            addUserDialog(requireContext(), myListAdapter)
        }

        return view
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment UsersFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            UsersFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }

    /**
     * Popup para adicionar um utilizador
     */
    @SuppressLint("SetTextI18n")
    private fun addUserDialog(context: Context, listAdapter: MainActivity.MyUserListAdapter) {
        val dialog = Dialog(context)
        //Remover título default
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        //Permitir o fecho do popup
        dialog.setCancelable(true)
        //Layout a ser utilizado no popup
        dialog.setContentView(R.layout.add_user)

        dialog.findViewById<TextView?>(R.id.user).setText("Criar Utilizador", TextView.BufferType.EDITABLE)


        val roles: ArrayList<String> = arrayListOf()
        roles.add("Empregado")
        roles.add("Gerente")

        val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, roles)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        val spinner: Spinner = dialog.findViewById(R.id.addUserRole)
        spinner.adapter = adapter

        val usernameEt: EditText = dialog.findViewById(R.id.addUserName)
        val passEt: EditText = dialog.findViewById(R.id.addUserPass)
        val passConfirmEt: EditText = dialog.findViewById(R.id.editUserConfirmPass)
        //listener para adicionar novo user
        val submitButton: Button = dialog.findViewById(R.id.submit_button)
        submitButton.setOnClickListener {
            val usernameText = usernameEt.text.toString()
            val pass = passEt.text.toString()
            val passConfirm = passConfirmEt.text.toString()
            var role = spinner.selectedItem.toString()
            //alterar o texto da role para o correto
            role = if (role.contains("Gerente")) {
                "manager"
            }else{
                "employee"
            }

            val myUsers = mainActivity.myUsers

            if (usernameText.isNotEmpty()) {

                if (pass.isNotEmpty()) {

                    if (pass == passConfirm) {
                        //preparar chamada ao API
                        val jsonObjectRequest = object : StringRequest(
                            Method.POST, apiUsersUrl,
                            { response ->

                                //Log.e("res", response.toString())
                                //caso a resposta do api contenha "existe um utilizador" então já existe um utilizador com o nome desejado
                                if (response.trim('"').contains("existe um utilizador")){
                                    Toast.makeText(context, response.trim('"'), Toast.LENGTH_SHORT).show()
                                } else {
                                    //caso não exista já um utilizador com esse nome, cria-se o utilizador e adicionamo-lo à lista
                                    Toast.makeText(context, "Utilizador Criado", Toast.LENGTH_SHORT).show()

                                    myUsers.add(User(response.trim('"').toInt(), usernameText, pass, role))

                                    listAdapter.notifyDataSetChanged()

                                    dialog.dismiss()
                                }

                            },
                            { error ->
                                Log.e("res", error.toString())
                                Toast.makeText(context, "Conecte-se à internet para criar o utilizador", Toast.LENGTH_SHORT).show()
                                dialog.dismiss()
                            }
                        ) {
                            override fun getBodyContentType(): String {
                                return "application/json; charset=utf-8"
                            }
                            //carregar as variáveis que pretendemos enviar para o API no body do pedido
                            override fun getBody(): ByteArray {
                                val jsonBody = JSONObject()
                                jsonBody.put("username", usernameText)
                                jsonBody.put("password", pass)
                                jsonBody.put("role", role)
                                return jsonBody.toString().toByteArray()
                            }

                        }

                        //Adicionar o pedido á fila
                        Volley.newRequestQueue(context).add(jsonObjectRequest)

                    }else{
                        Toast.makeText(context, "As palavras-passe não são iguais", Toast.LENGTH_SHORT).show()
                    }

                }else{
                    passEt.error = "A Palavra-Passe é obrigatória"
                }

            }else{
                usernameEt.error = "O Nome de Utilizador é obrigatório"
            }

            mainActivity.hideKeyboard()
        }

        dialog.show()
    }

    /**
     * Popup para edição de um user
     */
    @SuppressLint("SetTextI18n")
    private fun editUserDialog(context: Context, user: User, listAdapter: MainActivity.MyUserListAdapter) {
        val dialog = Dialog(context)
        //remover o titulo default
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        //Permitir o fecho do popup
        dialog.setCancelable(true)
        //Layout a utilizar no popup
        dialog.setContentView(R.layout.edit_user)

        dialog.findViewById<TextView?>(R.id.user).setText("Editar Utilizador", TextView.BufferType.EDITABLE)

        val userName = user.getUsername()
        dialog.findViewById<TextView?>(R.id.editUserName).setText(userName, TextView.BufferType.EDITABLE)

        val roles: ArrayList<String> = arrayListOf()
        if (user.getRole().contains("manager")) {
            roles.add("Gerente")
            roles.add("Empregado")
        }else{
            roles.add("Empregado")
            roles.add("Gerente")
        }

        val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, roles)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        val spinner: Spinner = dialog.findViewById(R.id.editUserRole)
        spinner.adapter = adapter

        val usernameEt: EditText = dialog.findViewById(R.id.editUserName)
        val passEt: EditText = dialog.findViewById(R.id.editUserPass)
        val passConfirmEt: EditText = dialog.findViewById(R.id.editUserConfirmPass)
        //listener para edição do user
        val submitButton: Button = dialog.findViewById(R.id.submit_button)
        submitButton.setOnClickListener {
            val usernameText = usernameEt.text.toString()
            var pass = passEt.text.toString()
            var passConfirm = passConfirmEt.text.toString()
            var role = spinner.selectedItem.toString()
            //alterar para o correto
            role = if (role.contains("Gerente")) {
                "manager"
            }else{
                "employee"
            }

            if (pass.isEmpty() && passConfirm.isEmpty()) {
                pass = user.getPassword()
                passConfirm = user.getPassword()
            }

            val myUsers = mainActivity.myUsers

            if (usernameText.isNotEmpty()) {

                if (pass.isNotEmpty()) {

                    if (pass == passConfirm) {

                        val url = "${apiUsersUrl}/${user.getUsername()}"
                        //preparar pedido para a API
                        val jsonObjectRequest = object : StringRequest(
                            Method.PUT, url,
                            { response ->

                                //Log.e("res", response.toString())
                                Toast.makeText(context, response.trim('"'), Toast.LENGTH_SHORT).show()
                                //caso a resposta do API contenha "foi eliminado" removemos o user e atualizamos a lista
                                if (response.trim('"').contains("foi eliminado")){
                                    myUsers.remove(user)
                                }

                                myUsers[myUsers.indexOf(user)] = User(user.getID(), usernameText, pass, role)

                                listAdapter.notifyDataSetChanged()

                            },
                            { error ->
                                Log.e("res", error.toString())
                                Toast.makeText(context, "Conecte-se à internet para editar o utilizador", Toast.LENGTH_SHORT).show()

                                mainActivity.refreshUsersList()
                                listAdapter.notifyDataSetChanged()
                            }
                        ) {
                            override fun getBodyContentType(): String {
                                return "application/json; charset=utf-8"
                            }
                            //carregar as variaveis que pretendemos enviar para o API no body do pedido
                            override fun getBody(): ByteArray {
                                val jsonBody = JSONObject()
                                jsonBody.put("username", usernameText)
                                jsonBody.put("password", pass)
                                jsonBody.put("role", role)
                                return jsonBody.toString().toByteArray()
                            }

                        }

                        //Adicionar o pedido à fila
                        Volley.newRequestQueue(context).add(jsonObjectRequest)

                        dialog.dismiss()

                    }else{
                        Toast.makeText(context, "As palavras-passe não são iguais", Toast.LENGTH_SHORT).show()
                    }

                }else{
                    passEt.error = "A Palavra-Passe é obrigatória"
                }

            }else{
                usernameEt.error = "O Nome de Utilizador é obrigatório"
            }

            mainActivity.hideKeyboard()
        }

        dialog.show()
    }

    /**
     * Popup para remover um user
     */
    private fun removeUserDialog(context: Context, user: User, listAdapter: MainActivity.MyUserListAdapter) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Deseja eliminar este utilizador?")

        val myUsers = mainActivity.myUsers

        builder.setPositiveButton("Sim") { dialog, which ->

            val url = "${apiUsersUrl}/${user.getUsername()}"
            //preparar pedido para o API
            val jsonArrayRequest = StringRequest(
                Request.Method.DELETE, url,
                { response ->
                    //removemos o user e atualizamos a lista
                    myUsers.remove(user)
                    listAdapter.notifyDataSetChanged()

                    Toast.makeText(context, response.trim('"'), Toast.LENGTH_SHORT).show()

                },
                { error ->

                    Toast.makeText(context, "Conecte-se à internet para eliminar o utilizador", Toast.LENGTH_SHORT).show()
                    mainActivity.refreshUsersList()
                    listAdapter.notifyDataSetChanged()

                }
            )

            //Adicionamos o pedido à fila
            Volley.newRequestQueue(context).add(jsonArrayRequest)
        }

        builder.setNegativeButton("Não", null)

        builder.show()
    }
}