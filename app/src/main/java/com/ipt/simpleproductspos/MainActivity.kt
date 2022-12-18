package com.ipt.simpleproductspos

import android.content.Context
import android.os.Bundle
import android.view.*
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import androidx.viewpager.widget.ViewPager
import androidx.appcompat.app.AppCompatActivity
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.LayoutRes
import com.ipt.simpleproductspos.ui.main.SectionsPagerAdapter
import com.ipt.simpleproductspos.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    var myProducts: ArrayList<Product> = arrayListOf()
    lateinit var myListAdapter: MyListAdapter
    var totalPrice: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        myListAdapter = MyListAdapter(this, R.layout.item_view, myProducts)

        val sectionsPagerAdapter = SectionsPagerAdapter(this, supportFragmentManager)
        val viewPager: ViewPager = binding.viewPager
        viewPager.adapter = sectionsPagerAdapter
        val tabs: TabLayout = binding.tabs
        tabs.setupWithViewPager(viewPager)

        /*val fab: FloatingActionButton = binding.fab
        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
        }*/
    }

    fun normalizePrice(price: Double): String {
        val text2D = String.format("%.2f", price).replace(",", ".")
        return text2D.split('.')[0] + "," + text2D.split('.')[1].padEnd(2, '0')
    }

    fun hideKeyboard(){
        val view = currentFocus
        if (view != null){
            val hideMe = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            hideMe.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    class MyListAdapter(context: Context, @LayoutRes private val layoutResource: Int, private val myProducts: List<Product>):
        ArrayAdapter<Product>(context, layoutResource, myProducts) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            // Make sure we have a view to work with (may have been given null)
            var itemView = convertView
            if (itemView == null) {
                itemView = LayoutInflater.from(context).inflate(R.layout.item_view, parent, false)
            }

            // Find the product to work with.
            val currentProduct: Product = myProducts.get(position)

            // Fill the view
            val imageView: ImageView = itemView?.findViewById<View>(R.id.item_icon) as ImageView
            imageView.setImageResource(currentProduct.getIconID())

            // Name:
            val nameText = itemView.findViewById<View>(R.id.item_txtProduct) as TextView
            nameText.text = currentProduct.getName()

            // Price:
            val priceText = itemView.findViewById<View>(R.id.item_txtPrice) as TextView
            val text2D = String.format("%.2f", currentProduct.getPrice()).replace(",", ".")
            priceText.text = text2D.split('.')[0] + "," + text2D.split('.')[1].padEnd(2, '0') + " €"

            // Quantity:
            val quantityText = itemView.findViewById<View>(R.id.item_txtQuantity) as TextView
            quantityText.text = currentProduct.getQuantity().toString()

            return itemView
        }
    }
}