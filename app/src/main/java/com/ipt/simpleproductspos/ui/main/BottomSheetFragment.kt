package com.ipt.simpleproductspos.ui.main

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.core.widget.doOnTextChanged
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.ipt.simpleproductspos.MainActivity
import com.ipt.simpleproductspos.R

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [BottomSheetFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class BottomSheetFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private lateinit var mainActivity: MainActivity
    lateinit var thisView: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }

        mainActivity = (activity as MainActivity)
    }

    /**
     * Atualiza e normaliza o preço do menu de checkout
     */
    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        thisView = view

        val totaPrice = mainActivity.totalPrice

        val totalTxt: TextView = view.findViewById(R.id.totalPrice)
        totalTxt.text = "${
            mainActivity.normalizePrice(totaPrice)
        } €"

    }

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view: View = inflater.inflate(R.layout.fragment_bottom_sheet, container, false)

        BottomSheetBehavior.from(view).apply {
            this.state = BottomSheetBehavior.STATE_COLLAPSED
        }

        val totaPrice = mainActivity.totalPrice

        val receivePrice: EditText = view.findViewById(R.id.receivePrice)
        receivePrice.doOnTextChanged { text, start, before, count ->

            view.findViewById<TextView?>(R.id.changePrice).setText("0,00 €", TextView.BufferType.EDITABLE)

            if (text != null)
                if(text.isNotEmpty() && text.toString().toDoubleOrNull() !== null){
                    if(text.toString().toDouble() - totaPrice >= 0) {

                        view.findViewById<TextView?>(R.id.changePrice).setText(
                            "${
                                mainActivity.normalizePrice(text.toString().toDouble() - totaPrice)
                            } €", TextView.BufferType.EDITABLE
                        )

                    }
                }
        }

        val but: Button = view.findViewById(R.id.btn_pay)
        but.setOnClickListener {

            if(mainActivity.totalPrice > 0.00) {

                mainActivity.myProducts.clear()
                mainActivity.totalPrice = 0.00

                mainActivity.myProductListAdapter.notifyDataSetChanged()

            }

            mainActivity.hideKeyboard()
            //atualiza e substitui o menu de checkout
            mainActivity.supportFragmentManager.beginTransaction().replace(R.id.bottom_sheet_fragment_parent, BottomSheetFragment()).commit()

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
         * @return A new instance of fragment BottomSheetFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            BottomSheetFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}