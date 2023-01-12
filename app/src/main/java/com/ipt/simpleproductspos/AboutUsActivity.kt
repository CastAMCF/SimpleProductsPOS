package com.ipt.simpleproductspos

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import com.ipt.simpleproductspos.databinding.ActivityAboutUsBinding
import com.ipt.simpleproductspos.ui.main.ViewPagerAdapter


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

        githubButton.setOnClickListener {
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
    }

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