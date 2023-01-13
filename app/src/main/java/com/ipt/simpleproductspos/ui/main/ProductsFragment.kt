package com.ipt.simpleproductspos.ui.main

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.net.UrlQuerySanitizer
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.*
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.ipt.simpleproductspos.User
import com.ipt.simpleproductspos.MainActivity
import com.ipt.simpleproductspos.Product
import com.ipt.simpleproductspos.R
import org.json.JSONObject


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [ProductsFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ProductsFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private lateinit var mainActivity: MainActivity
    private lateinit var apiProductsUrl: String
    private lateinit var session: User

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
        mainActivity = (activity as MainActivity)
        apiProductsUrl = mainActivity.apiProductsUrl
        session = mainActivity.session
    }

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view: View = inflater.inflate(R.layout.fragment_products, container, false)

        mainActivity.layout = view.findViewById(R.id.product_card_list)

        //Botão para adicionar um novo produto
        val addProduct: FloatingActionButton = view.findViewById(R.id.add_product_card)
        //Caso o user seja gerente, adicionamos um listener para adicionar um produto, caso contrário escondemos o botão
        if (session.getRole().contains("manager")) {
            addProduct.setOnClickListener { view ->
                addProductDataDialog(requireContext())
            }
        }else{
            addProduct.isVisible = false
        }
        //atualizar a lista de produtos
        mainActivity.refreshProductsList()

        return view
    }

    /**
     * Popup para adicionar um novo produto
     */
    @SuppressLint("SetTextI18n")
    private fun addProductDataDialog(context: Context) {
        val dialog = Dialog(context)
        //Desativar titulo default
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        //Permitir o fecho do popup
        dialog.setCancelable(true)
        //Layout a ser utilizado no popup
        dialog.setContentView(R.layout.edit_product)

        dialog.findViewById<TextView?>(R.id.product).setText("Adicionar Produto", TextView.BufferType.EDITABLE)

        val nameEt: EditText = dialog.findViewById(R.id.nameInput)
        val priceEt: EditText = dialog.findViewById(R.id.priceInput)
        mainActivity.addImage = dialog.findViewById(R.id.image)

        mainActivity.addImage.setOnClickListener{ mainActivity.chooseImgTypeDialog(context) }

        val submitButton: Button = dialog.findViewById(R.id.submit_button)
        submitButton.text = "Adicionar"
        //listener para adicionar um novo produto
        submitButton.setOnClickListener {
            val name = nameEt.text.toString()
            val price = priceEt.text

            val sanitizer = UrlQuerySanitizer();
            sanitizer.allowUnregisteredParamaters = true;
            sanitizer.parseUrl("http://example.com/?name=${name}");
            var icon = sanitizer.getValue("name")


            if(name.isNotEmpty() && name != "placeholder") {

                if (price != null)
                    if(price.isNotEmpty() && price.toString().toDoubleOrNull() != null) {

                        val priceNum = price.toString().replace(",", ".").toDouble()
                        //preparar pedido para o API
                        val jsonObjectRequest = object : StringRequest(
                            Method.POST, apiProductsUrl,
                            { addResponse ->

                                val url = "${apiProductsUrl}/${addResponse.trim('"').toInt()}"
                                icon += addResponse.trim('"').toInt()
                                //preparar segundo pedido para o api com a resposta do primeiro pedido
                                val jsonObjectRequest = object : StringRequest(
                                    Method.PUT, url,
                                    { response ->

                                        Toast.makeText(context, "Produto Adicionado", Toast.LENGTH_SHORT).show()
                                        //converter o icon devolvido pelo primeiro pedido em um bitmap
                                        mainActivity.saveImage(icon,
                                            mainActivity.convertToBitmap(
                                                mainActivity.addImage.drawable,
                                                mainActivity.addImage.drawable.intrinsicWidth,
                                                mainActivity.addImage.drawable.intrinsicHeight)
                                        )
                                        //adicionar a vista do produto criado
                                        mainActivity.addProductView(Product(addResponse.trim('"').toInt(), icon, name, 0, priceNum))

                                    },
                                    { }
                                ) {
                                    override fun getBodyContentType(): String {
                                        return "application/json; charset=utf-8"
                                    }
                                    //carregar as variaveis que pretendemos enviar para o API no body do pedido
                                    override fun getBody(): ByteArray {
                                        val jsonBody = JSONObject()
                                        jsonBody.put("icon", icon)
                                        jsonBody.put("name", name)
                                        jsonBody.put("quantity", 0)
                                        jsonBody.put("price", priceNum)
                                        return jsonBody.toString().toByteArray()
                                    }

                                }

                                //Adicionar o segundo pedido à fila
                                Volley.newRequestQueue(context).add(jsonObjectRequest)

                            },
                            { error ->
                                Log.e("res", error.toString())
                                Toast.makeText(context, "Conecte-se à internet para adicionar o produto", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            override fun getBodyContentType(): String {
                                return "application/json; charset=utf-8"
                            }
                            //carregar as variaveis que pretendemos enviar para o API no body do pedido
                            override fun getBody(): ByteArray {
                                val jsonBody = JSONObject()
                                jsonBody.put("icon", icon)
                                jsonBody.put("name", name)
                                jsonBody.put("quantity", 0)
                                jsonBody.put("price", priceNum)
                                return jsonBody.toString().toByteArray()
                            }

                        }

                        //Adicionar o primeiro pedido à fila
                        Volley.newRequestQueue(context).add(jsonObjectRequest)

                        mainActivity.hideKeyboard()

                        dialog.dismiss()

                    }else{
                        priceEt.error = "O Preço é obrigatório"
                    }

            }else{
                nameEt.error = "O Nome é obrigatório"
            }

        }

        dialog.show()
    }


    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment ProdutosFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            ProductsFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }

        /**
         * Popup para adicionar um produto ao carrinho
         */
        @SuppressLint("SetTextI18n")
        fun addProductDialog(context: Context, productItem: Product) {
            val dialog = Dialog(context)
            //Desativar titulo default
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            //Permitir o fecho do popup
            dialog.setCancelable(true)
            //Layout a ser utilizado no popup
            dialog.setContentView(R.layout.add_product)

            val mainActivity = (context as MainActivity)
            val productName = productItem.getName()
            var price = productItem.getPrice()
            val image = productItem.getIcon()

            dialog.findViewById<TextView?>(R.id.product).setText(productName, TextView.BufferType.EDITABLE)
            dialog.findViewById<TextView?>(R.id.price).setText("Preço: 0,00 €", TextView.BufferType.EDITABLE)

            val quantityEt: EditText = dialog.findViewById(R.id.quantity)
            val priceEt: EditText = dialog.findViewById(R.id.priceInput)

            if(price > 0){
                dialog.findViewById<TextView?>(R.id.price).setText("Preço: ${mainActivity.normalizePrice(price)} €", TextView.BufferType.EDITABLE)

                quantityEt.doOnTextChanged { text, start, before, count ->
                    if (text != null)
                        if(text.isNotEmpty() && text.toString().toDoubleOrNull() != null) {
                            //normalizar o preço com a quantidade
                            dialog.findViewById<TextView?>(R.id.price).setText("Preço: ${
                                mainActivity.normalizePrice(price * text.toString().toInt())
                            } €", TextView.BufferType.EDITABLE)

                        }
                }

                priceEt.isVisible = false

            }else{

                priceEt.doOnTextChanged { text, start, before, count ->
                    if (text != null)
                        if(text.isNotEmpty() && text.toString().toDoubleOrNull() != null){
                            if(quantityEt.text.isNotEmpty() && quantityEt.text.toString().toDoubleOrNull() != null) {
                                //normalizar o preço com a quantidade
                                dialog.findViewById<TextView?>(R.id.price).setText(
                                    "Preço: ${
                                        mainActivity.normalizePrice(text.toString().toDouble() * quantityEt.text.toString().toInt())
                                    } €", TextView.BufferType.EDITABLE)

                            }else{
                                val num = text.toString().replace(",", ".")

                                if(num.contains(".")){
                                    //normalizar o preço com a quantidade
                                    dialog.findViewById<TextView?>(R.id.price).setText(
                                        "Preço: ${
                                            mainActivity.normalizePrice(text.toString().toDouble())
                                        } €", TextView.BufferType.EDITABLE)
                                }else{
                                    dialog.findViewById<TextView?>(R.id.price).setText(
                                        "Preço: ${
                                            "$text,00"
                                        } €", TextView.BufferType.EDITABLE)
                                }
                            }
                        }
                }

                quantityEt.doOnTextChanged { text, start, before, count ->
                    if (text != null)
                        if(text.isNotEmpty() && text.toString().toDoubleOrNull() != null) {

                            if (priceEt.text != null)
                                if(priceEt.text.isNotEmpty() && priceEt.text.toString().toDoubleOrNull() != null) {
                                    //normalizar o preço com a quantidade
                                    dialog.findViewById<TextView?>(R.id.price).setText("Preço: ${
                                        mainActivity.normalizePrice(priceEt.text.toString().toDouble() * text.toString().toInt())
                                    } €", TextView.BufferType.EDITABLE)

                                }else{
                                    //normalizar o preço com a quantidade
                                    dialog.findViewById<TextView?>(R.id.price).setText("Preço: ${
                                        mainActivity.normalizePrice(price * text.toString().toInt())
                                    } €", TextView.BufferType.EDITABLE)

                                }
                        }
                }
            }
            //listener para adicionar o produto ao carrinho
            val submitButton: Button = dialog.findViewById(R.id.submit_button)
            submitButton.setOnClickListener {
                if (quantityEt.text.toString().isNotEmpty()) {

                    if (priceEt.isVisible) {
                        if (priceEt.text.toString().isEmpty()) {
                            priceEt.error = "Preço é obrigatório"
                            return@setOnClickListener
                        } else {
                            price = priceEt.text.toString().replace(",", ".").toDouble()
                        }
                    }

                    var quantify = quantityEt.text.toString().toInt()
                    var product = Product(0, image, productName, quantify, price * quantify)
                    //atualizar os produtos no carrinho e respetivo preço total
                    if (mainActivity.myProducts.any { x -> x.getName() == productName }) {
                        val i =
                            mainActivity.myProducts.indexOfFirst { x -> x.getName() == productName }

                        quantify += mainActivity.myProducts[i].getQuantity()
                        product = Product(0, image, productName, quantify, price * quantify)

                        mainActivity.totalPrice -= mainActivity.myProducts[i].getPrice()

                        mainActivity.myProducts[i] = product
                    } else {
                        mainActivity.myProducts.add(product)
                    }

                    mainActivity.totalPrice += price * quantify
                    //refazer o menu de checkout
                    mainActivity.supportFragmentManager.beginTransaction()
                        .replace(R.id.bottom_sheet_fragment_parent, BottomSheetFragment()).commit()

                    mainActivity.hideKeyboard()

                    dialog.dismiss()
                } else {
                    priceEt.error = "Preço é obrigatório"
                }
            }

            dialog.show()
        }
    }
}