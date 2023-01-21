package com.ipt.simpleproductspos.ui.activity

import android.R.attr.left
import android.R.attr.right
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import com.ipt.simpleproductspos.R
import com.ipt.simpleproductspos.databinding.ActivityAboutUsBinding
import com.ipt.simpleproductspos.ui.fragment.ViewPagerAdapter


class AboutUsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAboutUsBinding
    private val imageList = arrayListOf(R.drawable.andre, R.drawable.afonso)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAboutUsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val viewPager = binding.idViewPager

        val vpAdapter = ViewPagerAdapter(this, imageList)
        viewPager.adapter = vpAdapter

        val githubButton : ImageButton = binding.githubButton
        //listener do botão do github
        githubButton.setOnClickListener {
            //consoante a imagem selecionada no viewpager, retorna o github da pessoa na imagem em questão
            if (viewPager.currentItem == 0) {
                val url = "https://github.com/JKZ02/"
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse(url)
                startActivity(intent)
            } else {
                val url = "https://github.com/CastAMCF/"
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse(url)
                startActivity(intent)
            }
        }

        val rightButton : ImageButton = binding.rightButton
        rightButton.setOnClickListener {

            if (viewPager.currentItem < viewPager.right)
                viewPager.setCurrentItem(viewPager.currentItem + 1, true)

        }

        val leftButton : ImageButton = binding.leftButton
        leftButton.setOnClickListener {

            if (viewPager.currentItem < viewPager.left)
                viewPager.setCurrentItem(viewPager.currentItem - 1, true)

        }
    }

    /**
     * Voltar ao login quando pressionado home
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}