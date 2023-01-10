package com.ipt.simpleproductspos

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.util.Base64
import android.util.Log
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.viewpager.widget.ViewPager
import com.android.volley.Request
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.tabs.TabLayout
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.ipt.simpleproductspos.databinding.ActivityMainBinding
import com.ipt.simpleproductspos.ui.main.BottomSheetFragment
import com.ipt.simpleproductspos.ui.main.ProductsFragment
import com.ipt.simpleproductspos.ui.main.SectionsPagerAdapter
import com.ipt.simpleproductspos.ui.main.TAB_TITLES
import org.json.JSONObject
import java.io.ByteArrayOutputStream


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    lateinit var session: User
    val apiProductsUrl = "https://nodeapidam-production.up.railway.app/products"
    val apiUsersUrl = "https://nodeapidam-production.up.railway.app/users"

    private var dataProducts: ArrayList<Product> = arrayListOf()
    lateinit var layout: GridLayout

    lateinit var myProductListAdapter: MyProductListAdapter
    lateinit var myUserListAdapter: MyUserListAdapter

    var myUsers: ArrayList<User> = arrayListOf()
    var myProducts: ArrayList<Product> = arrayListOf()
    var totalPrice: Double = 0.0

    lateinit var pickLauncher: ActivityResultLauncher<Intent>
    lateinit var cameraLauncher: ActivityResultLauncher<Intent>
    lateinit var requestPermissionLauncher: ActivityResultLauncher<String>
    lateinit var addImage: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        session = intent.getSerializableExtra("sessao") as User

        myProductListAdapter = MyProductListAdapter(this, R.layout.item_product_view, myProducts)
        myUserListAdapter = MyUserListAdapter(this, R.layout.item_user_view, myUsers)

        saveImage("placeholder", BitmapFactory.decodeResource(resources, R.drawable.placeholder))

        TAB_TITLES = if (session.getRole().contains("manager")) {
            arrayOf(
                R.string.tab_text_1,
                R.string.users
            )
        } else {
            arrayOf(
                R.string.tab_text_1,
                R.string.tab_text_2
            )
        }

        val sectionsPagerAdapter = SectionsPagerAdapter(this, supportFragmentManager, session.getRole().contains("manager"))
        val viewPager: ViewPager = binding.viewPager
        viewPager.adapter = sectionsPagerAdapter
        val tabs: TabLayout = binding.tabs
        tabs.setupWithViewPager(viewPager)

        cameraLauncher =
            registerForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) { result ->
                if (result.resultCode == RESULT_OK && result.data != null) {

                    val bundle: Bundle? = result.data!!.extras
                    val bitmap = bundle?.get("data") as Bitmap?

                    if(bitmap != null)
                        addImage.setImageBitmap(bitmap)

                }
            }

        pickLauncher =
            registerForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) { result ->
                if (result.resultCode == RESULT_OK && result.data != null) {

                    val selectedImage: Uri? = result.data?.data

                    if(selectedImage != null)
                        addImage.setImageURI(selectedImage)
                }
            }

        val requestPermissionIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        requestPermissionLauncher =
            registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                if (isGranted) {
                    // Permission is granted. Continue the action or workflow in your app.
                    pickLauncher.launch(requestPermissionIntent)
                } else {
                    // Explain to the user that the feature is unavailable because the
                    // feature requires a permission that the user has denied. At the
                    // same time, respect the user's decision. Don't link to system
                    // settings in an effort to convince the user to change their decision.

                    val alertBuilder = AlertDialog.Builder(this)
                    alertBuilder.setMessage("Não é possível escolher imagens guardadas, conceda a permissão e tente novamente")
                        .setCancelable(false)
                        .setPositiveButton("OK", null)
                        .setNegativeButton("Conceder") { dialog, which ->
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            val uri = Uri.fromParts("package", packageName, null)
                            intent.data = uri
                            startActivity(intent)
                        }
                    val alert = alertBuilder.create()
                    alert.show()
                }
            }

        val logoutButton = binding.logoutButton

        logoutButton.setOnClickListener{
            session = User(0,"","","")
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }

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

    fun saveImage(icon: String, image: Bitmap){
        val baos = ByteArrayOutputStream()
        image.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val byte: ByteArray = baos.toByteArray()

        val encodedImage: String = Base64.encodeToString(byte, Base64.DEFAULT)

        val shre: SharedPreferences = getSharedPreferences("images", MODE_PRIVATE)
        val edit: SharedPreferences.Editor = shre.edit()
        edit.putString(icon, encodedImage)
        edit.apply()
    }

    @SuppressLint("SetTextI18n")
    private fun editProductDataDialog(
        context: Context, view: View, product: Product, imageView: ImageView, but: Button, nameView: TextView, priceView: TextView
    ) {
        val dialog = Dialog(context)
        //We have added a title in the custom layout. So let's disable the default title.
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        //The user will be able to cancel the dialog bu clicking anywhere outside the dialog.
        dialog.setCancelable(true)
        //Mention the name of the layout of your custom dialog.
        dialog.setContentView(R.layout.edit_product)

        dialog.findViewById<TextView?>(R.id.product).setText("Editar Produto", TextView.BufferType.EDITABLE)

        //Initializing the views of the dialog
        val nameEt: EditText = dialog.findViewById(R.id.nameInput)
        val priceEt: EditText = dialog.findViewById(R.id.priceInput)
        addImage = dialog.findViewById(R.id.image)

        addImage.setOnClickListener{ chooseImgTypeDialog(context) }

        nameEt.setText(product.getName(), TextView.BufferType.EDITABLE)
        priceEt.setText(product.getPrice().toString(), TextView.BufferType.EDITABLE)
        addImage.setImageBitmap(product.getBitmap(context))

        val submitButton: Button = dialog.findViewById(R.id.submit_button)
        submitButton.setOnClickListener {
            val name = nameEt.text.toString()
            val price = priceEt.text
            val icon = product.getIcon()

            saveImage(icon, convertToBitmap(addImage.drawable, addImage.drawable.intrinsicWidth, addImage.drawable.intrinsicHeight))

            if(name.isNotEmpty()) {

                if (price != null)
                    if(price.isNotEmpty() && price.toString().toDoubleOrNull() != null) {

                        val url = "${apiProductsUrl}/${product.getID()}"
                        val priceNum = price.toString().replace(",", ".").toDouble()

                        val jsonObjectRequest = object : StringRequest(
                            Method.PUT, url,
                            { response ->

                                //Log.e("res", response.toString())
                                Toast.makeText(context, response.trim('"'), Toast.LENGTH_SHORT).show()

                                if (response.trim('"').contains("foi eliminado")){
                                    removeProductView(view, product)
                                    refreshProductsList()
                                } else {
                                    val updateProduct = Product(product.getID(), icon, name, 0, priceNum)

                                    generateProductView(updateProduct, imageView, but, nameView, priceView, view)

                                    if (myProducts.any { x -> x.getName() == product.getName() }){
                                        val i = myProducts.indexOfFirst { x -> x.getName() == product.getName() }

                                        val quantity = myProducts[i].getQuantity()
                                        val newProduct = Product(product.getID(), icon, name, quantity, priceNum * quantity)

                                        totalPrice -= myProducts[i].getPrice()

                                        totalPrice += priceNum * quantity

                                        myProducts[i] = newProduct

                                        supportFragmentManager.beginTransaction().replace(R.id.bottom_sheet_fragment_parent, BottomSheetFragment()).commit()
                                        myProductListAdapter.notifyDataSetChanged()
                                    }
                                }

                            },
                            { error ->
                                Log.e("res", error.toString())
                                Toast.makeText(context, "Conecte-se à internet para editar o produto", Toast.LENGTH_SHORT).show()

                                refreshProductsList()
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


                        hideKeyboard()

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

    private fun removeProductDataDialog(context: Context, view: View, product: Product) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Deseja eliminar este produto?")

        builder.setPositiveButton("Sim") { dialog, which ->

            val url = "${apiProductsUrl}/${product.getID()}"

            val jsonArrayRequest = StringRequest(
                Request.Method.DELETE, url,
                { response ->

                    removeProductView(view, product)

                    if (response.trim('"').contains("foi eliminado"))
                        refreshProductsList()

                    Toast.makeText(context, response.trim('"'), Toast.LENGTH_SHORT).show()

                },
                { error ->

                    Toast.makeText(context, "Conecte-se à internet para eliminar o produto", Toast.LENGTH_SHORT).show()
                    refreshProductsList()

                }
            )

            // Access the RequestQueue through your singleton class.
            Volley.newRequestQueue(context).add(jsonArrayRequest)
        }

        builder.setNegativeButton("Não", null)

        builder.show()
    }

    fun addProductView(dataProduct: Product): View {
        val productView: View = layoutInflater.inflate(R.layout.product_card, null)

        val image: ImageView = productView.findViewById(R.id.product_image)
        val price: TextView = productView.findViewById(R.id.product_price)
        val name: TextView = productView.findViewById(R.id.product_name)
        val but: Button = productView.findViewById(R.id.product_but)

        generateProductView(dataProduct, image, but, name, price, productView)

        layout.addView(productView)

        return productView
    }

    private fun removeProductView(view: View, product: Product) {

        layout.removeView(view)

        if (myProducts.any { x -> x.getName() == product.getName() }) {
            val i = myProducts.indexOfFirst { x -> x.getName() == product.getName() }

            totalPrice -= myProducts[i].getPrice()

            myProducts.removeAt(i)

            supportFragmentManager.beginTransaction()
                .replace(R.id.bottom_sheet_fragment_parent, BottomSheetFragment()).commit()
            myProductListAdapter.notifyDataSetChanged()
        }

    }

    @SuppressLint("SetTextI18n")
    private fun generateProductView(product: Product, imageView: ImageView, but: Button, nameView: TextView, priceView: TextView, view: View) {

        if(product.getBitmap(this) == null)
            saveImage(product.getIcon(), BitmapFactory.decodeResource(resources, R.drawable.placeholder))

        imageView.setImageBitmap(product.getBitmap(this))

        priceView.text = "${
            normalizePrice(product.getPrice())
        } €"

        nameView.text = product.getName()

        but.setOnLongClickListener {

            if (session.getRole().contains("manager")) {
                removeProductDataDialog(this, view, product)
            }

            true
        }

        but.setOnClickListener{

            if (session.getRole().contains("manager")) {
                editProductDataDialog(this, view, product, imageView, but, nameView, priceView)
            }else{
                ProductsFragment.addProductDialog(this, product)
            }

        }

    }

    fun chooseImgTypeDialog(context: Context) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Adiconar Imagem")

        val items = arrayOf("Escolher Imagem", "Câmara")

        builder.setItems(items) { dialog, which ->
            hideKeyboard()
            when (which) {
                0 -> {
                    val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)

                    try {

                        when (PackageManager.PERMISSION_GRANTED) {
                            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                            -> {
                                // You can use the API that requires the permission.
                                pickLauncher.launch(intent)
                            }
                            else -> {
                                // You can directly ask for the permission.
                                // The registered ActivityResultCallback gets the result of this request.
                                requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                            }
                        }

                    }catch (e: Exception){
                        Toast.makeText(context, "Não há uma aplicação que suporta esta ação", Toast.LENGTH_SHORT).show()
                    }

                    dialog.dismiss()
                }
                1 -> {
                    val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

                    try {
                        cameraLauncher.launch(intent)
                    }catch (e: Exception){
                        Toast.makeText(context, "Não há uma aplicação que suporta esta ação", Toast.LENGTH_SHORT).show()
                    }

                    dialog.dismiss()
                }
            }
        }

        val typeMatDialog = builder.create()
        typeMatDialog.show()
    }

    fun convertToBitmap(drawable: Drawable, widthPixels: Int, heightPixels: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(widthPixels, heightPixels, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, widthPixels, heightPixels)
        drawable.draw(canvas)
        return bitmap
    }

    fun refreshProductsList() {
        dataProducts.clear()
        layout.removeAllViews()

        val jsonArrayRequest = JsonArrayRequest(
            Request.Method.GET, apiProductsUrl, null,
            { response ->

                val products: Array<Product> = Gson().fromJson(response.toString(), object : TypeToken<Array<Product>>() {}.type)
                products.forEachIndexed  { idx, product -> dataProducts.add(product) }

                val icons: ArrayList<String> = arrayListOf()

                for (product in dataProducts) {
                    addProductView(product)
                    icons.add(product.getIcon())
                }

                val shre: SharedPreferences = getSharedPreferences("images", MODE_PRIVATE)
                val edit: SharedPreferences.Editor = shre.edit()

                val allEntries: Map<String, *> = shre.all
                for ((key, value) in allEntries) {

                    if (!icons.contains(key)) {
                        edit.remove(key)
                        edit.apply()
                    }
                }

            },
            { error ->
                Toast.makeText(this, "Conecte-se à internet para obter os produtos guardados", Toast.LENGTH_SHORT).show()
            }
        )

        // Access the RequestQueue through your singleton class.
        Volley.newRequestQueue(this).add(jsonArrayRequest)
    }

    fun refreshUsersList() {
        myUsers.clear()

        val jsonArrayRequest = JsonArrayRequest(
            Request.Method.GET, "${apiUsersUrl}?username=${session.getUsername()}&password=${session.getPassword()}", null,
            { response ->

                val users: Array<User> = Gson().fromJson(response.toString(), object : TypeToken<Array<User>>() {}.type)
                users.forEachIndexed  { idx, user -> myUsers.add(user) }

            },
            { error ->
                Toast.makeText(this, "Conecte-se à internet para obter os utilizadores", Toast.LENGTH_SHORT).show()
            }
        )

        // Access the RequestQueue through your singleton class.
        Volley.newRequestQueue(this).add(jsonArrayRequest)
    }

    class MyProductListAdapter(context: Context, @LayoutRes private val layoutResource: Int, private val myProducts: List<Product>):
        ArrayAdapter<Product>(context, layoutResource, myProducts) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            // Make sure we have a view to work with (may have been given null)
            var itemView = convertView
            if (itemView == null) {
                itemView = LayoutInflater.from(context).inflate(R.layout.item_product_view, parent, false)
            }

            // Find the product to work with.
            val currentProduct: Product = myProducts[position]

            // Fill the view
            val imageView: ImageView = itemView?.findViewById<View>(R.id.item_icon) as ImageView
            imageView.setImageBitmap(currentProduct.getBitmap(context))

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

    class MyUserListAdapter(context: Context, @LayoutRes private val layoutResource: Int, private val myUsers: List<User>):
        ArrayAdapter<User>(context, layoutResource, myUsers) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            // Make sure we have a view to work with (may have been given null)
            var itemView = convertView
            if (itemView == null) {
                itemView = LayoutInflater.from(context).inflate(R.layout.item_user_view, parent, false)
            }

            // Find the product to work with.
            val currentUser: User = myUsers[position]

            // Fill the view
            // Name:
            val nameText = itemView?.findViewById<View>(R.id.item_txtUsername) as TextView
            nameText.text = currentUser.getUsername()

            // Password:
            val passText = itemView.findViewById<View>(R.id.item_txtPass) as TextView
            passText.text = currentUser.getPassword()

            // Role:
            val roleText = itemView.findViewById<View>(R.id.item_txtRole) as TextView
            var role = currentUser.getRole()

            role = if (role.contains("manager")) {
                "Gerente"
            }else{
                "Empregado"
            }

            roleText.text = role

            return itemView
        }
    }
}