package com.ipt.simpleproductspos.ui.main

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import com.ipt.simpleproductspos.MainActivity
import com.ipt.simpleproductspos.Product
import com.ipt.simpleproductspos.R


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
    private var dataProducts: ArrayList<Product> = arrayListOf()
    private lateinit var mainActivity: MainActivity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }

        mainActivity = (activity as MainActivity)
        populatedProductsList()
    }

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view: View = inflater.inflate(R.layout.fragment_products, container, false)

        val layout: GridLayout = view.findViewById(R.id.product_card_list)

        for (i in dataProducts.indices) {
            val productView: View = inflater.inflate(R.layout.product_card, null)

            val image: ImageView = productView.findViewById(R.id.product_image)
            image.setImageResource(dataProducts[i].getIconID())

            val price: TextView = productView.findViewById(R.id.product_price)
            price.text = "${
                mainActivity.normalizePrice(dataProducts[i].getPrice())
            } €"

            val name: TextView = productView.findViewById(R.id.product_name)
            name.text = dataProducts[i].getName()

            val but: Button = productView.findViewById(R.id.product_but)

            but.setOnLongClickListener {
                dataProducts[i] = Product(R.drawable.placeholder, "Test", 0, 1.50)

                image.setImageResource(dataProducts[i].getIconID())

                name.text = dataProducts[i].getName()

                price.text = "${
                    mainActivity.normalizePrice(dataProducts[i].getPrice())
                } €"

                true
            }

            when (i) {
                0 -> but.setOnClickListener{ breadTypeDialog("Pão", "Trigo", dataProducts[i].getIconID()) }
                1 -> but.setOnClickListener{ breadTypeDialog("Broa", "Milho", dataProducts[i].getIconID()) }
                2 -> but.setOnClickListener{ halfBreadTypeDialog() }
                7 -> but.setOnClickListener{ sweetTypeDialog() }
                else -> but.setOnClickListener{ addProductDialog(requireContext(), dataProducts[i]) }
            }

            layout.addView(productView)
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
    }

    private fun breadTypeDialog(type: String, material: String, image: Int) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Tipo de $type")

        val items = arrayOf(material, "$material com centeio")
        val checkedItem = 0

        builder.setSingleChoiceItems(items, checkedItem) { dialog, which ->
            addProductDialog(requireContext(), Product(image, "$type de ${items[which]}", 0, 2.00))
            dialog.dismiss()
        }

        val typeMatDialog = builder.create()
        typeMatDialog.show()
    }

    private fun halfBreadTypeDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Tipo de Metade")

        val items = arrayOf("Trigo", "Trigo com centeio", "Milho", "Milho com centeio")
        val checkedItem = 0

        builder.setSingleChoiceItems(items, checkedItem) { dialog, which ->
            addProductDialog(requireContext(), Product(R.drawable.placeholder, "Metade de ${items[which]}", 0, 1.00))
            dialog.dismiss()
        }

        val typeMatDialog = builder.create()
        typeMatDialog.show()
    }

    private fun sweetTypeDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Tipo de Compra")

        val items = arrayOf("Unidade", "Caixa")
        val checkedItem = 0

        builder.setSingleChoiceItems(items, checkedItem) { dialog, which ->
            when (which) {
                0 -> {
                    addProductDialog(requireContext(), Product(R.drawable.placeholder, "Pastel de Chícharo", 0, 1.20))
                    dialog.dismiss()
                }
                1 -> {
                    addProductDialog(requireContext(), Product(R.drawable.placeholder, "Caixa de Pasteis de Chícharo", 0, 6.00))
                    dialog.dismiss()
                }
            }
        }

        val typeMatDialog = builder.create()
        typeMatDialog.show()
    }

    @SuppressLint("SetTextI18n")
    private fun addProductDialog(context: Context, productItem: Product) {
        val dialog = Dialog(context)
        //We have added a title in the custom layout. So let's disable the default title.
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        //The user will be able to cancel the dialog bu clicking anywhere outside the dialog.
        dialog.setCancelable(true)
        //Mention the name of the layout of your custom dialog.
        dialog.setContentView(R.layout.add_product)

        val productName = productItem.getName()
        val price = productItem.getPrice()
        val image = productItem.getIconID()

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
            var product = Product(image, productName, quantify, price * quantify)

            if (mainActivity.myProducts.any { x -> x.getName() == productName }){
                val i = mainActivity.myProducts.indexOfFirst { x -> x.getName() == productName }

                quantify += mainActivity.myProducts[i].getQuantity()
                product = Product(image, productName, quantify, price * quantify)

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

    private fun populatedProductsList() {
        dataProducts.add(Product(R.drawable.placeholder, getString(R.string.pao), 0, 2.00))
        dataProducts.add(Product(R.drawable.placeholder, getString(R.string.broa), 0, 2.00))
        dataProducts.add(Product(R.drawable.placeholder, getString(R.string.metadePao), 0, 1.00))
        dataProducts.add(Product(R.drawable.placeholder, getString(R.string.bolas), 0, 0.22))
        dataProducts.add(Product(R.drawable.placeholder, getString(R.string.paoChou), 0, 0.95))
        dataProducts.add(Product(R.drawable.placeholder, getString(R.string.broaAzeit), 0, 0.90))
        dataProducts.add(Product(R.drawable.placeholder, getString(R.string.bolos), 0, 0.90))
        dataProducts.add(Product(R.drawable.placeholder, getString(R.string.pastelChi), 0, 1.20))
        dataProducts.add(Product(R.drawable.placeholder, getString(R.string.boloRei), 0, 0.00))
        dataProducts.add(Product(R.drawable.placeholder, getString(R.string.boloRainha), 0, 0.00))
        dataProducts.add(Product(R.drawable.placeholder, getString(R.string.abobora), 0, 0.00))
        dataProducts.add(Product(R.drawable.placeholder, getString(R.string.saco), 0, 0.10))
    }
}