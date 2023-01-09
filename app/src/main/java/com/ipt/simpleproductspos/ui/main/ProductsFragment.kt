package com.ipt.simpleproductspos.ui.main

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.net.UrlQuerySanitizer
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.floatingactionbutton.FloatingActionButton
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
 * Use the [ProdutosFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ProdutosFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private lateinit var mainActivity: MainActivity
    private lateinit var apiProductsUrl: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }

        mainActivity = (activity as MainActivity)
        apiProductsUrl = mainActivity.apiProductsUrl
    }

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view: View = inflater.inflate(R.layout.fragment_products, container, false)

        val addProduct: FloatingActionButton = view.findViewById(R.id.add_product_card)

        addProduct.setOnClickListener { view ->
            addProductDataDialog(requireContext())
        }

        val options: FloatingActionButton = view.findViewById(R.id.options)

        options.setOnClickListener { view ->
            optionsDialog(requireContext())
        }

        mainActivity.layout = view.findViewById(R.id.product_card_list)

        mainActivity.refreshProductsList()

        return view
    }

    @SuppressLint("SetTextI18n")
    private fun addProductDataDialog(context: Context) {
        val dialog = Dialog(context)
        //We have added a title in the custom layout. So let's disable the default title.
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        //The user will be able to cancel the dialog bu clicking anywhere outside the dialog.
        dialog.setCancelable(true)
        //Mention the name of the layout of your custom dialog.
        dialog.setContentView(R.layout.edit_product)

        dialog.findViewById<TextView?>(R.id.product).setText("Adicionar Produto", TextView.BufferType.EDITABLE)

        //Initializing the views of the dialog
        val nameEt: EditText = dialog.findViewById(R.id.nameInput)
        val priceEt: EditText = dialog.findViewById(R.id.priceInput)
        mainActivity.addImage = dialog.findViewById(R.id.image)

        mainActivity.addImage.setOnClickListener{ mainActivity.chooseImgTypeDialog(context) }

        val submitButton: Button = dialog.findViewById(R.id.submit_button)
        submitButton.text = "Adicionar"

        submitButton.setOnClickListener {
            val name = nameEt.text.toString()
            val price = priceEt.text
            val priceNum = price.toString().replace(",", ".").toDouble()

            val sanitizer = UrlQuerySanitizer();
            sanitizer.allowUnregisteredParamaters = true;
            sanitizer.parseUrl("http://example.com/?name=${name}");
            var icon = sanitizer.getValue("name")

            //layout.removeViewAt(0)

            if(name.isNotEmpty() && name != "placeholder") {

                if (price != null)
                    if(price.isNotEmpty() && price.toString().toDoubleOrNull() != null) {

                        val jsonObjectRequest = object : StringRequest(
                            Method.POST, apiProductsUrl,
                            { addResponse ->

                                //Log.e("res", response.toString())

                                val url = "${apiProductsUrl}/${addResponse.trim('"').toInt()}"
                                icon += addResponse.trim('"').toInt()

                                val jsonObjectRequest = object : StringRequest(
                                    Method.PUT, url,
                                    { response ->

                                        Toast.makeText(context, "Produto Adicionado", Toast.LENGTH_SHORT).show()

                                        mainActivity.saveImage(icon,
                                            mainActivity.convertToBitmap(
                                                mainActivity.addImage.drawable,
                                                mainActivity.addImage.drawable.intrinsicWidth,
                                                mainActivity.addImage.drawable.intrinsicHeight)
                                        )

                                        mainActivity.addProductView(Product(addResponse.trim('"').toInt(), icon, name, 0, priceNum))

                                    },
                                    { }
                                ) {
                                    override fun getBodyContentType(): String {
                                        return "application/json; charset=utf-8"
                                    }

                                    override fun getBody(): ByteArray {
                                        val jsonBody = JSONObject()
                                        jsonBody.put("icon", icon)
                                        jsonBody.put("name", name)
                                        jsonBody.put("quantity", 0)
                                        jsonBody.put("price", priceNum)
                                        return jsonBody.toString().toByteArray()
                                    }

                                }

                                // Access the RequestQueue through your singleton class.
                                Volley.newRequestQueue(context).add(jsonObjectRequest)

                            },
                            { error ->
                                Log.e("res", error.toString())
                                Toast.makeText(context, "Conecte-se à internet para criar o produto", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            override fun getBodyContentType(): String {
                                return "application/json; charset=utf-8"
                            }

                            override fun getBody(): ByteArray {
                                val jsonBody = JSONObject()
                                jsonBody.put("icon", icon)
                                jsonBody.put("name", name)
                                jsonBody.put("quantity", 0)
                                jsonBody.put("price", priceNum)
                                return jsonBody.toString().toByteArray()
                            }

                        }

                        // Access the RequestQueue through your singleton class.
                        Volley.newRequestQueue(context).add(jsonObjectRequest)

                        //refreshProductsList()


                        mainActivity.hideKeyboard()

                        dialog.dismiss()

                    }else{
                        Toast.makeText(context, "O Preço é obrigatório", Toast.LENGTH_SHORT).show()
                    }

            }else{
                Toast.makeText(context, "O Nome é obrigatório", Toast.LENGTH_SHORT).show()
            }

        }

        dialog.show()
    }

    private fun optionsDialog(context: Context){
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Opções")

        val items = arrayOf("Editar", "Eliminar")
        val checkedItem = if (mainActivity.deleteOp) 1 else 0

        builder.setSingleChoiceItems(items, checkedItem) { dialog, which ->
            when (which) {
                0 -> {
                    mainActivity.deleteOp = false
                    dialog.dismiss()
                }
                1 -> {
                    mainActivity.deleteOp = true
                    dialog.dismiss()
                }
            }
        }

        val typeMatDialog = builder.create()
        typeMatDialog.show()
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
            ProdutosFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }

        fun breadTypeDialog(context: Context, type: String, material: String, image: String) {
            val builder = AlertDialog.Builder(context)
            builder.setTitle("Tipo de $type")

            val items = arrayOf(material, "$material com centeio")

            builder.setItems(items) { dialog, which ->
                addProductDialog(context, Product(0, image, "$type de ${items[which]}", 0, 2.00))
                dialog.dismiss()
            }

            val typeMatDialog = builder.create()
            typeMatDialog.show()
        }

        fun halfBreadTypeDialog(context: Context) {
            val builder = AlertDialog.Builder(context)
            builder.setTitle("Tipo de Metade")

            val items = arrayOf("Trigo", "Trigo com centeio", "Milho", "Milho com centeio")

            builder.setItems(items) { dialog, which ->
                addProductDialog(context, Product(0, "placeholder", "Metade de ${items[which]}", 0, 1.00))
                dialog.dismiss()
            }

            val typeMatDialog = builder.create()
            typeMatDialog.show()
        }

        fun sweetTypeDialog(context: Context) {
            val builder = AlertDialog.Builder(context)
            builder.setTitle("Tipo de Compra")

            val items = arrayOf("Unidade", "Caixa")

            builder.setItems(items) { dialog, which ->
                when (which) {
                    0 -> {
                        addProductDialog(context, Product(0, "placeholder", "Pastel de Chícharo", 0, 1.20))
                        dialog.dismiss()
                    }
                    1 -> {
                        addProductDialog(context, Product(0, "placeholder", "Caixa de Pasteis de Chícharo", 0, 6.00))
                        dialog.dismiss()
                    }
                }
            }

            val typeMatDialog = builder.create()
            typeMatDialog.show()
        }

        @SuppressLint("SetTextI18n")
        fun addProductDialog(context: Context, productItem: Product) {
            val dialog = Dialog(context)
            //We have added a title in the custom layout. So let's disable the default title.
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            //The user will be able to cancel the dialog bu clicking anywhere outside the dialog.
            dialog.setCancelable(true)
            //Mention the name of the layout of your custom dialog.
            dialog.setContentView(R.layout.add_product)

            val mainActivity = (context as MainActivity)
            val productName = productItem.getName()
            val price = productItem.getPrice()
            val image = productItem.getIcon()

            dialog.findViewById<TextView?>(R.id.product).setText(productName, TextView.BufferType.EDITABLE)
            dialog.findViewById<TextView?>(R.id.price).setText("Preço: 0,00 €", TextView.BufferType.EDITABLE)

            //Initializing the views of the dialog
            val quantityEt: EditText = dialog.findViewById(R.id.quantity)
            val priceEt: EditText = dialog.findViewById(R.id.priceInput)

            if(price > 0){
                dialog.findViewById<TextView?>(R.id.price).setText("Preço: ${mainActivity.normalizePrice(price)} €", TextView.BufferType.EDITABLE)

                quantityEt.doOnTextChanged { text, start, before, count ->
                    if (text != null)
                        if(text.isNotEmpty() && text.toString().toDoubleOrNull() != null) {

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

                                dialog.findViewById<TextView?>(R.id.price).setText(
                                    "Preço: ${
                                        mainActivity.normalizePrice(text.toString().toDouble() * quantityEt.text.toString().toInt())
                                    } €", TextView.BufferType.EDITABLE)

                            }else{
                                val num = text.toString().replace(",", ".")

                                if(num.contains(".")){
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

                                    dialog.findViewById<TextView?>(R.id.price).setText("Preço: ${
                                        mainActivity.normalizePrice(priceEt.text.toString().toDouble() * text.toString().toInt())
                                    } €", TextView.BufferType.EDITABLE)

                                }else{

                                    dialog.findViewById<TextView?>(R.id.price).setText("Preço: ${
                                        mainActivity.normalizePrice(price * text.toString().toInt())
                                    } €", TextView.BufferType.EDITABLE)

                                }
                        }
                }
            }

            val submitButton: Button = dialog.findViewById(R.id.submit_button)
            submitButton.setOnClickListener {
                var quantify = quantityEt.text.toString().toInt()
                var product = Product(0, image, productName, quantify, price * quantify)

                if (mainActivity.myProducts.any { x -> x.getName() == productName }){
                    val i = mainActivity.myProducts.indexOfFirst { x -> x.getName() == productName }

                    quantify += mainActivity.myProducts[i].getQuantity()
                    product = Product(0, image, productName, quantify, price * quantify)

                    mainActivity.totalPrice -= mainActivity.myProducts[i].getPrice()

                    mainActivity.myProducts[i] = product
                }else{
                    mainActivity.myProducts.add(product)
                }

                mainActivity.totalPrice += price * quantify

                mainActivity.supportFragmentManager.beginTransaction().replace(R.id.bottom_sheet_fragment_parent, BottomSheetFragment()).commit()

                mainActivity.hideKeyboard()

                dialog.dismiss()
            }

            dialog.show()
        }
    }
}