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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.ipt.simpleproductspos.MainActivity
import com.ipt.simpleproductspos.Product
import com.ipt.simpleproductspos.R


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [SaleFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class SaleFragment : Fragment() {
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
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view: View = inflater.inflate(R.layout.fragment_sale, container, false)

        mainActivity.hideKeyboard()

        val list: ListView = view.findViewById(R.id.productsListView)
        val myListAdapter =  mainActivity.myProductListAdapter
        list.adapter = myListAdapter
        //listener para atualizar ao "puxar" para baixo
        val refreshLayout: SwipeRefreshLayout = view.findViewById(R.id.refreshLayout)
        refreshLayout.setOnRefreshListener {

            myListAdapter.notifyDataSetChanged()
            mainActivity.supportFragmentManager.beginTransaction().replace(R.id.bottom_sheet_fragment_parent, BottomSheetFragment()).commit()

            refreshLayout.isRefreshing = false
        }
        //listener para atualizar um produto do carrinho ao pressionar
        list.setOnItemClickListener { adapterView, view, i, l ->

            val selectProduct: Product = list.getItemAtPosition(i) as Product
            editProductDialog(requireContext(), selectProduct, myListAdapter)

        }
        //listener para remover um produto do carrinho ao manter pressionado
        list.setOnItemLongClickListener { adapterView, view, i, l ->

            val myProducts = mainActivity.myProducts

            val builder = AlertDialog.Builder(requireContext())
            builder.setTitle("Deseja eliminar este produto da lista?")

            builder.setPositiveButton("Sim") { dialog, which ->

                mainActivity.totalPrice -= myProducts[i].getPrice()
                mainActivity.supportFragmentManager.beginTransaction().replace(R.id.bottom_sheet_fragment_parent, BottomSheetFragment()).commit()
                //removemos o produto e atualizamos a lista
                myProducts.removeAt(i)
                myListAdapter.notifyDataSetChanged()
            }

            builder.setNegativeButton("Não", null)

            builder.show()

            true
        }
        //refazer o menu de pagamento
        mainActivity.supportFragmentManager.beginTransaction().replace(R.id.bottom_sheet_fragment_parent, BottomSheetFragment()).commit()

        return view
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment SalesFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            SaleFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }

    /**
     * Popup para adicionar um pedido ao carrinho de compras
     */
    @SuppressLint("SetTextI18n")
    private fun editProductDialog(context: Context, productItem: Product, adapter: MainActivity.MyProductListAdapter) {
        val dialog = Dialog(context)
        //Desativar titulo default
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        //Permitir o fecho do popup
        dialog.setCancelable(true)
        //Layout a utilizar no popup
        dialog.setContentView(R.layout.add_product)

        val productName = productItem.getName()
        val price = productItem.getPrice()
        val image = productItem.getIcon()
        val quantity = productItem.getQuantity()
        val correctPrice = price / quantity

        dialog.findViewById<TextView?>(R.id.product).setText(productName, TextView.BufferType.EDITABLE)
        dialog.findViewById<TextView?>(R.id.price).setText("Preço: ${mainActivity.normalizePrice(correctPrice)} €", TextView.BufferType.EDITABLE)

        val quantityEt: EditText = dialog.findViewById(R.id.quantity)
        quantityEt.doOnTextChanged { text, start, before, count ->
            if (text != null)
                if(text.isNotEmpty() && text.toString().toDoubleOrNull() != null) {
                    //normalizar o preço consoante a quantidade introduzida
                    dialog.findViewById<TextView?>(R.id.price).setText("Preço: ${
                        mainActivity.normalizePrice(correctPrice * text.toString().toInt())
                    } €", TextView.BufferType.EDITABLE)

                }else{
                    //normalizar o preço
                    dialog.findViewById<TextView?>(R.id.price).setText("Preço: ${
                        mainActivity.normalizePrice(correctPrice)
                    } €", TextView.BufferType.EDITABLE)

                }
        }

        quantityEt.setText(quantity.toString(), TextView.BufferType.EDITABLE)

        val priceEt: EditText = dialog.findViewById(R.id.priceInput)
        priceEt.isVisible = false
        //listener para adicionar um produto ao carrinho
        val submitButton: Button = dialog.findViewById(R.id.submit_button)
        submitButton.text = getString(R.string.save)
        submitButton.setOnClickListener {
            if (quantityEt.text.toString().isNotEmpty()) {
                val quantify = quantityEt.text.toString().toInt()
                val product = Product(0, image, productName, quantify, correctPrice * quantify)
                //atualizar o preço do checkout
                mainActivity.totalPrice -= price

                mainActivity.myProducts[mainActivity.myProducts.indexOf(productItem)] = product

                mainActivity.totalPrice += correctPrice * quantify

                mainActivity.supportFragmentManager.beginTransaction().replace(R.id.bottom_sheet_fragment_parent, BottomSheetFragment()).commit()

                mainActivity.hideKeyboard()

                dialog.dismiss()
                //atualizar o menu de checkout
                adapter.notifyDataSetChanged()
            } else {
                quantityEt.error = "Quantidade é obrigatória"
            }
        }

        dialog.show()
    }
}