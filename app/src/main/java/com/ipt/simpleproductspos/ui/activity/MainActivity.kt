package com.ipt.simpleproductspos.ui.activity

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
import androidx.core.widget.doOnTextChanged
import androidx.viewpager.widget.ViewPager
import com.android.volley.Response
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textfield.TextInputLayout
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.ipt.simpleproductspos.*
import com.ipt.simpleproductspos.data.Product
import com.ipt.simpleproductspos.data.Session
import com.ipt.simpleproductspos.data.User
import com.ipt.simpleproductspos.databinding.ActivityMainBinding
import com.ipt.simpleproductspos.ui.fragment.BottomSheetFragment
import com.ipt.simpleproductspos.ui.fragment.ProductsFragment
import com.ipt.simpleproductspos.ui.adapter.SectionsPagerAdapter
import com.ipt.simpleproductspos.ui.adapter.TAB_TITLES
import com.ipt.simpleproductspos.volley.VolleyRequest
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    //Sessão passada do login
    var session: User = Session().getUser()
    //produtos a mostrar
    private var dataProducts: ArrayList<Product> = arrayListOf()
    lateinit var layout: GridLayout

    lateinit var myProductListAdapter: MyProductListAdapter
    lateinit var myUserListAdapter: MyUserListAdapter

    var myUsers: ArrayList<User> = Session().getUsers()
    var myProducts: ArrayList<Product> = Session().getProducts()

    //preparar os launchers para as chamadas a:
    //Escolha de imagens da galeria
    lateinit var pickLauncher: ActivityResultLauncher<Intent>
    //Utilização da camera
    lateinit var cameraLauncher: ActivityResultLauncher<Intent>
    //Autorização de permissões
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>
    lateinit var addImage: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        myProductListAdapter = MyProductListAdapter(this, R.layout.item_product_view, myProducts)
        myUserListAdapter = MyUserListAdapter(this, R.layout.item_user_view, myUsers)
        //Guardar imagem encriptada na internal storage
        val shre: SharedPreferences = getSharedPreferences("images", MODE_PRIVATE)
        val allEntries: Map<String, *> = shre.all
        for ((key, value) in allEntries) {

            if (!key.contains("placeholder")) {
                saveImage("placeholder", BitmapFactory.decodeResource(resources,
                    R.drawable.placeholder
                ))
            }
        }

        //Alterar o nome dos fragmentos baseado na role do utilizador atual
        TAB_TITLES = if (session.role.contains("manager")) {
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

        val sectionsPagerAdapter = SectionsPagerAdapter(this, supportFragmentManager, session.role.contains("manager"))
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
        dialog.setContentView(R.layout.update_product)

        dialog.findViewById<TextView?>(R.id.product).setText("Editar Produto", TextView.BufferType.EDITABLE)

        //Variáveis utilizadas no popup
        val nameEt: EditText = dialog.findViewById(R.id.nameInput)
        val nameLayout: TextInputLayout = dialog.findViewById(R.id.nameInputLayout)
        val priceEt: EditText = dialog.findViewById(R.id.priceInput)
        val priceLayout: TextInputLayout = dialog.findViewById(R.id.priceInputLayout)
        addImage = dialog.findViewById(R.id.image)
        //Popup para alterar imagem ao carregar na imagem atual
        addImage.setOnClickListener{ chooseImgTypeDialog(context) }
        //Definir os valores atuais

        nameEt.setText(product.name, TextView.BufferType.EDITABLE)
        priceEt.setText(product.price.toString(), TextView.BufferType.EDITABLE)
        addImage.setImageBitmap(product.getBitmap(context))

        nameEt.doOnTextChanged { text, start, before, count ->
            nameLayout.error = null
        }

        priceEt.doOnTextChanged { text, start, before, count ->
            priceLayout.error = null
        }

        val submitButton: Button = dialog.findViewById(R.id.submit_button)
        //listener para submeter alterações no produto
        submitButton.setOnClickListener {
            val name = nameEt.text.toString()
            val price = priceEt.text
            val icon = product.icon

            saveImage(icon, convertToBitmap(addImage.drawable, addImage.drawable.intrinsicWidth, addImage.drawable.intrinsicHeight))

            if(name.isNotEmpty()) {

                if (price != null)
                    if(price.isNotEmpty() && price.toString().toDoubleOrNull() != null) {

                        val priceNum = price.toString().replace(",", ".").toDouble()
                        //Efetuar pedido ao API
                        val response = Response.Listener<String> { response ->
                            //Caso a resposta do API contenha "foi eliminado, remove-se a vista do produto e refresca-se a lista dos produtos
                            if (response.trim('"').contains("foi eliminado")){
                                removeProductView(view, product)
                                refreshProductsList()
                            } else {
                                val updateProduct = Product(product.id, icon, name, 0, priceNum)
                                //gera a view para o produto atualizado
                                generateProductView(updateProduct, imageView, but, nameView, priceView, view)
                                //Caso o produto atualizado esteja no carrinho de compras, atualizamos o preço do mesmo
                                if (myProducts.any { x -> x.name == product.name }){
                                    val i = myProducts.indexOfFirst { x -> x.name == product.name }

                                    val quantity = myProducts[i].quantity
                                    val newProduct = Product(product.id, icon, name, quantity, priceNum * quantity)

                                    Session().subTotalPrice(myProducts[i].price)

                                    Session().addTotalPrice(priceNum * quantity)

                                    myProducts[i] = newProduct

                                    supportFragmentManager.beginTransaction().replace(R.id.bottom_sheet_fragment_parent, BottomSheetFragment()).commit()
                                    myProductListAdapter.notifyDataSetChanged()
                                }
                            }
                        }

                        val responseError = Response.ErrorListener { error ->
                            //sem conexão
                            Log.e("res", error.toString())
                            Toast.makeText(context, "Conecte-se à internet para editar o produto", Toast.LENGTH_SHORT).show()

                            refreshProductsList()
                        }

                        val jsonBody = JSONObject()
                        jsonBody.put("icon", icon)
                        jsonBody.put("name", name)
                        jsonBody.put("quantity", 0)
                        jsonBody.put("price", priceNum)

                        VolleyRequest().Product().update(this, product.id, response, responseError, jsonBody)


                        hideKeyboard()
                        //fechamos o popup
                        dialog.dismiss()

                    }else{
                        priceLayout.error = "O Preço é obrigatório"
                    }

            }else{
                nameLayout.error = "O Nome é obrigatório"
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

            //preparar pedido para o API
            val response = Response.Listener<String> { response ->
                //elimina o produto
                removeProductView(view, product)
                //Caso o produto tenha sido eliminado da bd, atualizamos a lista
                if (response.trim('"').contains("foi eliminado"))
                    refreshProductsList()

                Toast.makeText(context, response.trim('"'), Toast.LENGTH_SHORT).show()
            }

            val responseError = Response.ErrorListener { error ->
                //sem internet
                Toast.makeText(context, "Conecte-se à internet para eliminar o produto", Toast.LENGTH_SHORT).show()
                refreshProductsList()
            }

            VolleyRequest().Product().delete(this, product.id, response, responseError)
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

        if (myProducts.any { x -> x.name == product.name }) {
            val i = myProducts.indexOfFirst { x -> x.name == product.name }

            Session().subTotalPrice(myProducts[i].price)

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
            saveImage(product.icon, BitmapFactory.decodeResource(resources,
                R.drawable.placeholder
            ))

        imageView.setImageBitmap(product.getBitmap(this))

        priceView.text = "${
            normalizePrice(product.price)
        } €"

        nameView.text = product.name
        //manter premido para remover produto caso seja gerente
        but.setOnLongClickListener {

            if (session.role.contains("manager")) {
                removeProductDataDialog(this, view, product)
            }

            true
        }
        //listener para o produto
        but.setOnClickListener{

            if (session.role.contains("manager")) {
                editProductDataDialog(this, view, product, imageView, but, nameView, priceView)
            }else{
                ProductsFragment.addProductCheckoutDialog(this, product)
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
        val response = Response.Listener<JSONArray> { response ->
            val products: Array<Product> = Gson().fromJson(response.toString(), object : TypeToken<Array<Product>>() {}.type)
            products.forEachIndexed  { idx, product -> dataProducts.add(product) }

            val icons: ArrayList<String> = arrayListOf()

            for (product in dataProducts) {
                addProductView(product)
                icons.add(product.icon)
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
        }

        val responseError = Response.ErrorListener { error ->
            Toast.makeText(this, "Conecte-se à internet para obter os produtos guardados", Toast.LENGTH_SHORT).show()
        }

        VolleyRequest().Product().getAll(this, response, responseError)
    }

    /**
     * Atualiza a lista de users
     */
    fun refreshUsersList() {
        myUsers.clear()
        //Preparar pedido para a API
        val response = Response.Listener<JSONArray> { response ->

            val users: Array<User> = Gson().fromJson(response.toString(), object : TypeToken<Array<User>>() {}.type)
            users.forEachIndexed  { idx, user -> myUsers.add(user) }

        }

        val responseError = Response.ErrorListener { error ->
            Toast.makeText(this, "Conecte-se à internet para obter os utilizadores", Toast.LENGTH_SHORT).show()
        }

        VolleyRequest().User().getAll(this, response, responseError)
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
            nameText.text = currentProduct.name

            // preço
            val priceText = itemView.findViewById<View>(R.id.item_txtPrice) as TextView
            val text2D = String.format("%.2f", currentProduct.price).replace(",", ".")
            priceText.text = text2D.split('.')[0] + "," + text2D.split('.')[1].padEnd(2, '0') + " €"

            // quantidade
            val quantityText = itemView.findViewById<View>(R.id.item_txtQuantity) as TextView
            quantityText.text = currentProduct.quantity.toString()

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

            // UserService que vamos usar
            var (id, username, password, role) = myUsers[position]

            // nome
            val nameText = itemView?.findViewById<View>(R.id.item_txtUsername) as TextView
            nameText.text = username

            // password
            val passText = itemView.findViewById<View>(R.id.item_txtPass) as TextView
            passText.text = password

            // role
            val roleText = itemView.findViewById<View>(R.id.item_txtRole) as TextView
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