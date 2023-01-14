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
    //Sessão passada do login
    lateinit var session: User
    //vars para conectar ao api
    val apiProductsUrl = "https://nodeapidam-production.up.railway.app/products"
    val apiUsersUrl = "https://nodeapidam-production.up.railway.app/users"
    //produtos a mostrar
    private var dataProducts: ArrayList<Product> = arrayListOf()
    lateinit var layout: GridLayout

    lateinit var myProductListAdapter: MyProductListAdapter
    lateinit var myUserListAdapter: MyUserListAdapter

    var myUsers: ArrayList<User> = arrayListOf()
    var myProducts: ArrayList<Product> = arrayListOf()
    var totalPrice: Double = 0.0

    //preparar os launchers para as chamadas a:
    //Escolha de imagens da galeria
    lateinit var pickLauncher: ActivityResultLauncher<Intent>
    //Utilização da camera
    lateinit var cameraLauncher: ActivityResultLauncher<Intent>
    //Autorização de permissões
    lateinit var requestPermissionLauncher: ActivityResultLauncher<String>
    lateinit var addImage: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        //Carregar a sessão passada do login
        session = intent.getSerializableExtra("sessao") as User


        myProductListAdapter = MyProductListAdapter(this, R.layout.item_product_view, myProducts)
        myUserListAdapter = MyUserListAdapter(this, R.layout.item_user_view, myUsers)
        //Guardar imagem encriptada na internal storage
        saveImage("placeholder", BitmapFactory.decodeResource(resources, R.drawable.placeholder))

        //Alterar o nome dos fragmentos baseado na role do utilizador atual
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
                    // caso a permissão seja concedida, procedemos com a inicialização
                    pickLauncher.launch(requestPermissionIntent)
                } else {

                    // caso a permissão seja negada, informamos que não é possível continuar com o pretendido
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
        //Listener para logout, retorna para o login e reseta a sessão
        logoutButton.setOnClickListener{
            session = User(0,"","","")
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }

    }

    /**
     * normaliza o preço, substitui ',' por '.' e formata com 2 casas decimais
     */
    fun normalizePrice(price: Double): String {
        val text2D = String.format("%.2f", price).replace(",", ".")
        return text2D.split('.')[0] + "," + text2D.split('.')[1].padEnd(2, '0')
    }

    /**
     * Esconde o teclado
     */
    fun hideKeyboard(){
        val view = currentFocus
        if (view != null){
            val hideMe = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            hideMe.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    /**
     * Guarda uma imagem encriptada na internal storage
     */
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

    /**
     * Mostra um popup para editar um dado produto da view e da base de dados
     */
    @SuppressLint("SetTextI18n")
    private fun editProductDataDialog(
        context: Context, view: View, product: Product, imageView: ImageView, but: Button, nameView: TextView, priceView: TextView
    ) {
        val dialog = Dialog(context)
        //Desativar o título default
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        //Possibilidade de fechar o popup
        dialog.setCancelable(true)
        //Layout do popup
        dialog.setContentView(R.layout.edit_product)

        dialog.findViewById<TextView?>(R.id.product).setText("Editar Produto", TextView.BufferType.EDITABLE)

        //Variáveis utilizadas no popup
        val nameEt: EditText = dialog.findViewById(R.id.nameInput)
        val priceEt: EditText = dialog.findViewById(R.id.priceInput)
        addImage = dialog.findViewById(R.id.image)
        //Popup para alterar imagem ao carregar na imagem atual
        addImage.setOnClickListener{ chooseImgTypeDialog(context) }
        //Definir os valores atuais
        nameEt.setText(product.getName(), TextView.BufferType.EDITABLE)
        priceEt.setText(product.getPrice().toString(), TextView.BufferType.EDITABLE)
        addImage.setImageBitmap(product.getBitmap(context))

        val submitButton: Button = dialog.findViewById(R.id.submit_button)
        //listener para submeter alterações no produto
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
                        //Efetuar pedido ao API
                        val jsonObjectRequest = object : StringRequest(
                            Method.PUT, url,
                            { response ->


                                //Caso a resposta do API contenha "foi eliminado, remove-se a vista do produto e refresca-se a lista dos produtos
                                if (response.trim('"').contains("foi eliminado")){
                                    removeProductView(view, product)
                                    refreshProductsList()
                                } else {
                                    val updateProduct = Product(product.getID(), icon, name, 0, priceNum)
                                    //gera a view para o produto atualizado
                                    generateProductView(updateProduct, imageView, but, nameView, priceView, view)
                                    //Caso o produto atualizado esteja no carrinho de compras, atualizamos o preço do mesmo
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
                                //sem conexão
                                Log.e("res", error.toString())
                                Toast.makeText(context, "Conecte-se à internet para editar o produto", Toast.LENGTH_SHORT).show()

                                refreshProductsList()
                            }
                        ) {
                            override fun getBodyContentType(): String {
                                return "application/json; charset=utf-8"
                            }
                            //Carregar as variáveis que pretendemos enviar para a API no body.
                            override fun getBody(): ByteArray {
                                val jsonBody = JSONObject()
                                jsonBody.put("icon", icon)
                                jsonBody.put("name", name)
                                jsonBody.put("quantity", 0)
                                jsonBody.put("price", priceNum)
                                return jsonBody.toString().toByteArray()
                            }

                        }

                        // Acedemos à fila e adicionamos o nosso pedido
                        Volley.newRequestQueue(context).add(jsonObjectRequest)

                        //refreshProductsList()


                        hideKeyboard()
                        //fechamos o popup
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

    /**
     * Mostra um popup para remover um dado produto da view e da base de dados
     */
    private fun removeProductDataDialog(context: Context, view: View, product: Product) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Deseja eliminar este produto?")

        builder.setPositiveButton("Sim") { dialog, which ->

            val url = "${apiProductsUrl}/${product.getID()}"
            //preparar pedido para o API
            val jsonArrayRequest = StringRequest(
                Request.Method.DELETE, url,
                { response ->
                    //
                    removeProductView(view, product)
                    //Caso o produto tenha sido eliminado da bd, atualizamos a lista
                    if (response.trim('"').contains("foi eliminado"))
                        refreshProductsList()

                    Toast.makeText(context, response.trim('"'), Toast.LENGTH_SHORT).show()

                },
                { error ->
                    //sem internet
                    Toast.makeText(context, "Conecte-se à internet para eliminar o produto", Toast.LENGTH_SHORT).show()
                    refreshProductsList()

                }
            )

            // Acedemos à fila e adicionamos o nosso pedido
            Volley.newRequestQueue(context).add(jsonArrayRequest)
        }

        builder.setNegativeButton("Não", null)

        builder.show()
    }

    /**
     * Adiciona um produto à vista "Produtos"
     */
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

    /**
     * Remove um produto da vista "Produtos"
     */
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

    /**
     * Cria uma vista de um produto
     */
    @SuppressLint("SetTextI18n")
    private fun generateProductView(product: Product, imageView: ImageView, but: Button, nameView: TextView, priceView: TextView, view: View) {

        if(product.getBitmap(this) == null)
            saveImage(product.getIcon(), BitmapFactory.decodeResource(resources, R.drawable.placeholder))

        imageView.setImageBitmap(product.getBitmap(this))

        priceView.text = "${
            normalizePrice(product.getPrice())
        } €"

        nameView.text = product.getName()
        //manter premido para remover produto caso seja gerente
        but.setOnLongClickListener {

            if (session.getRole().contains("manager")) {
                removeProductDataDialog(this, view, product)
            }

            true
        }
        //listener para o produto
        but.setOnClickListener{

            if (session.getRole().contains("manager")) {
                editProductDataDialog(this, view, product, imageView, but, nameView, priceView)
            }else{
                ProductsFragment.addProductDialog(this, product)
            }

        }

    }

    /**
     * Inicia o popup para escolher uma imagem/capturar uma com a camara
     */
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

    /**
     * Converte uma imagem num bitmap
     */
    fun convertToBitmap(drawable: Drawable, widthPixels: Int, heightPixels: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(widthPixels, heightPixels, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, widthPixels, heightPixels)
        drawable.draw(canvas)
        return bitmap
    }

    /**
     * Atualiza a lista de produtos
     */
    fun refreshProductsList() {
        dataProducts.clear()
        layout.removeAllViews()
        //Preparar pedido para a API
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

        // Adicionar o pedido à fila
        Volley.newRequestQueue(this).add(jsonArrayRequest)
    }

    /**
     * Atualiza a lista de users
     */
    fun refreshUsersList() {
        myUsers.clear()
        //Preparar pedido para a API
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

        // Adicionar pedido à fila
        Volley.newRequestQueue(this).add(jsonArrayRequest)
    }

	/**
	 * Código adaptado de: https://www.youtube.com/watch?v=WRANgDgM2Zg&ab_channel=BrianFraser
	 */
    class MyProductListAdapter(context: Context, @LayoutRes private val layoutResource: Int, private val myProducts: List<Product>):
        ArrayAdapter<Product>(context, layoutResource, myProducts) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            // Utilizar a view passada, caso contrário encontrá-la
            var itemView = convertView
            if (itemView == null) {
                itemView = LayoutInflater.from(context).inflate(R.layout.item_product_view, parent, false)
            }

            // Produto que vamos usar
            val currentProduct: Product = myProducts[position]

            // Preenche a view
            val imageView: ImageView = itemView?.findViewById<View>(R.id.item_icon) as ImageView
            imageView.setImageBitmap(currentProduct.getBitmap(context))

            // nome
            val nameText = itemView.findViewById<View>(R.id.item_txtProduct) as TextView
            nameText.text = currentProduct.getName()

            // preço
            val priceText = itemView.findViewById<View>(R.id.item_txtPrice) as TextView
            val text2D = String.format("%.2f", currentProduct.getPrice()).replace(",", ".")
            priceText.text = text2D.split('.')[0] + "," + text2D.split('.')[1].padEnd(2, '0') + " €"

            // quantidade
            val quantityText = itemView.findViewById<View>(R.id.item_txtQuantity) as TextView
            quantityText.text = currentProduct.getQuantity().toString()

            return itemView
        }
    }

	/**
	 * Código adaptado de: https://www.youtube.com/watch?v=WRANgDgM2Zg&ab_channel=BrianFraser
	 */
    class MyUserListAdapter(context: Context, @LayoutRes private val layoutResource: Int, private val myUsers: List<User>):
        ArrayAdapter<User>(context, layoutResource, myUsers) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            // Utilizar a view passada, caso contrário encontrá-la
            var itemView = convertView
            if (itemView == null) {
                itemView = LayoutInflater.from(context).inflate(R.layout.item_user_view, parent, false)
            }

            // User que vamos usar
            val currentUser: User = myUsers[position]

            // nome
            val nameText = itemView?.findViewById<View>(R.id.item_txtUsername) as TextView
            nameText.text = currentUser.getUsername()

            // password
            val passText = itemView.findViewById<View>(R.id.item_txtPass) as TextView
            passText.text = currentUser.getPassword()

            // role
            val roleText = itemView.findViewById<View>(R.id.item_txtRole) as TextView
            var role = currentUser.getRole()
            //Display da role mais "user friendly"
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