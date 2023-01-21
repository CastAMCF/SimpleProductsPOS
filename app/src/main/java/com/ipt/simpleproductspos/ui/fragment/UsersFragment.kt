package com.ipt.simpleproductspos.ui.fragment

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
import android.widget.AdapterView.OnItemClickListener
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.android.volley.Response
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputLayout
import com.ipt.simpleproductspos.ui.activity.MainActivity
import com.ipt.simpleproductspos.R
import com.ipt.simpleproductspos.data.User
import com.ipt.simpleproductspos.volley.VolleyRequest
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }

        mainActivity = (activity as MainActivity)

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

        myListAdapter.notifyDataSetChanged()

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
        dialog.setContentView(R.layout.update_user)

        dialog.findViewById<TextView?>(R.id.user).setText("Criar Utilizador", TextView.BufferType.EDITABLE)


        val roles: ArrayList<String> = arrayListOf("Empregado", "Gerente")

        val adapter = ArrayAdapter(context, R.layout.spinner_item, roles)

        val roleLayout: TextInputLayout = dialog.findViewById(R.id.userRoleLayout)
        val autoCompleteTxt: AutoCompleteTextView = dialog.findViewById(R.id.userRole)
        autoCompleteTxt.setAdapter(adapter)

        var role = ""
        autoCompleteTxt.onItemClickListener = OnItemClickListener { parent, view, position, id ->
            roleLayout.error = null
            val selectedItem = parent.getItemAtPosition(position).toString()
            //alterar o texto da role para o correto
            role = if (selectedItem.contains("Gerente")) {
                "manager"
            }else{
                "employee"
            }
        }

        val usernameEt: EditText = dialog.findViewById(R.id.userName)
        val usernameLayout: TextInputLayout = dialog.findViewById(R.id.userNameLayout)
        val passEt: EditText = dialog.findViewById(R.id.userPass)
        val passLayout: TextInputLayout = dialog.findViewById(R.id.userPassLayout)
        val passConfirmEt: EditText = dialog.findViewById(R.id.userConfirmPass)

        usernameEt.doOnTextChanged { text, start, before, count ->
            usernameLayout.error = null
        }

        passEt.doOnTextChanged { text, start, before, count ->
            passLayout.error = null
        }

        //listener para adicionar novo user
        val submitButton: Button = dialog.findViewById(R.id.submit_button)
        submitButton.setOnClickListener {
            val usernameText = usernameEt.text.toString()
            val pass = passEt.text.toString()
            val passConfirm = passConfirmEt.text.toString()

            val myUsers = mainActivity.myUsers

            if (usernameText.isNotEmpty()) {

                if (pass.isNotEmpty()) {
                    if (pass == passConfirm) {

                        if (role.isNotEmpty()) {
                            //preparar chamada ao API
                            val response = Response.Listener<String> { response ->
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
                            }

                            val responseError = Response.ErrorListener { error ->
                                Log.e("res", error.toString())
                                Toast.makeText(context, "Conecte-se à internet para criar o utilizador", Toast.LENGTH_SHORT).show()
                                dialog.dismiss()
                            }

                            val jsonBody = JSONObject()
                            jsonBody.put("username", usernameText)
                            jsonBody.put("password", pass)
                            jsonBody.put("role", role)

                            VolleyRequest().User().create(context, response, responseError, jsonBody)

                        }else{
                            roleLayout.error = "Nenhum cargo selecionado"
                        }

                    }else{
                        Toast.makeText(context, "As palavras-passe não são iguais", Toast.LENGTH_SHORT).show()
                    }
                }else{
                    passLayout.error = "A Palavra-Passe é obrigatória"
                }

            }else{
                usernameLayout.error = "O Nome de Utilizador é\nobrigatório"
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
        dialog.setContentView(R.layout.update_user)

        dialog.findViewById<TextView?>(R.id.user).setText("Editar Utilizador", TextView.BufferType.EDITABLE)

        val (id, username, password, role) = user
        dialog.findViewById<TextView?>(R.id.userName).setText(username, TextView.BufferType.EDITABLE)


        val roles: ArrayList<String> = arrayListOf("Empregado", "Gerente")

        val adapter = ArrayAdapter(context, R.layout.spinner_item, roles)

        val roleLayout: TextInputLayout = dialog.findViewById(R.id.userRoleLayout)
        val autoCompleteTxt: AutoCompleteTextView = dialog.findViewById(R.id.userRole)
        autoCompleteTxt.setAdapter(adapter)

        var newRole = ""
        autoCompleteTxt.onItemClickListener = OnItemClickListener { parent, view, position, id ->
            roleLayout.error = null
            val selectedItem = parent.getItemAtPosition(position).toString()
            //alterar o texto da role para o correto
            newRole = if (selectedItem.contains("Gerente")) {
                "manager"
            }else{
                "employee"
            }
        }

        val usernameEt: EditText = dialog.findViewById(R.id.userName)
        val usernameLayout: TextInputLayout = dialog.findViewById(R.id.userNameLayout)
        val passEt: EditText = dialog.findViewById(R.id.userPass)
        val passLayout: TextInputLayout = dialog.findViewById(R.id.userPassLayout)
        val passConfirmEt: EditText = dialog.findViewById(R.id.userConfirmPass)

        usernameEt.doOnTextChanged { text, start, before, count ->
            usernameLayout.error = null
        }

        passEt.doOnTextChanged { text, start, before, count ->
            passLayout.error = null
        }

        /**/

        //listener para edição do user
        val submitButton: Button = dialog.findViewById(R.id.submit_button)
        submitButton.text = getString(R.string.save)
        submitButton.setOnClickListener {
            val usernameText = usernameEt.text.toString()
            var pass = passEt.text.toString()
            var passConfirm = passConfirmEt.text.toString()

            if (pass.isEmpty() && passConfirm.isEmpty()) {
                pass = password
                passConfirm = password
            }

            if (newRole.isEmpty())
                newRole = role

            val myUsers = mainActivity.myUsers

            if (usernameText.isNotEmpty()) {

                if (pass.isNotEmpty()) {
                    if (pass == passConfirm) {

                        if (newRole.isNotEmpty()) {

                            //preparar pedido para a API
                            val response = Response.Listener<String> { response ->
                                //Log.e("res", response.toString())
                                Toast.makeText(context, response.trim('"'), Toast.LENGTH_SHORT).show()
                                //caso a resposta do API contenha "foi eliminado" removemos o user e atualizamos a lista
                                if (response.trim('"').contains("foi eliminado")){
                                    myUsers.remove(user)
                                }

                                myUsers[myUsers.indexOf(user)] = User(id, usernameText, pass, newRole)

                                listAdapter.notifyDataSetChanged()
                            }

                            val responseError = Response.ErrorListener { error ->
                                Log.e("res", error.toString())
                                Toast.makeText(context, "Conecte-se à internet para editar o utilizador", Toast.LENGTH_SHORT).show()

                                mainActivity.refreshUsersList()
                                listAdapter.notifyDataSetChanged()
                            }

                            val jsonBody = JSONObject()
                            jsonBody.put("username", usernameText)
                            jsonBody.put("password", pass)
                            jsonBody.put("role", newRole)

                            VolleyRequest().User().update(context, username, response, responseError, jsonBody)

                            dialog.dismiss()

                        }else{
                            roleLayout.error = "Nenhum cargo selecionado"
                        }

                    }else{
                        Toast.makeText(context, "As palavras-passe não são iguais", Toast.LENGTH_SHORT).show()
                    }
                }else{
                    passLayout.error = "A Palavra-Passe é obrigatória"
                }

            }else{
                usernameLayout.error = "O Nome de Utilizador é\nobrigatório"
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

            //preparar pedido para o API
            val response = Response.Listener<String> { response ->
                //removemos o user e atualizamos a lista
                myUsers.remove(user)
                listAdapter.notifyDataSetChanged()

                Toast.makeText(context, response.trim('"'), Toast.LENGTH_SHORT).show()
            }

            val responseError = Response.ErrorListener { error ->
                Toast.makeText(context, "Conecte-se à internet para eliminar o utilizador", Toast.LENGTH_SHORT).show()
                mainActivity.refreshUsersList()
                listAdapter.notifyDataSetChanged()
            }

            VolleyRequest().User().delete(context, user.username, response, responseError)
        }

        builder.setNegativeButton("Não", null)

        builder.show()
    }
}