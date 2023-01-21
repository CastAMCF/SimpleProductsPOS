package com.ipt.simpleproductspos.ui.adapter

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.ipt.simpleproductspos.R
import com.ipt.simpleproductspos.ui.fragment.ProductsFragment
import com.ipt.simpleproductspos.ui.fragment.SaleFragment
import com.ipt.simpleproductspos.ui.fragment.UsersFragment

var TAB_TITLES = arrayOf(
    R.string.tab_text_1,
    R.string.tab_text_2
)

/**
 * A [FragmentPagerAdapter] that returns a fragment corresponding to
 * one of the sections/tabs/pages.
 */
class SectionsPagerAdapter(private val context: Context, fm: FragmentManager, private val admin: Boolean)
    : FragmentPagerAdapter(fm) {
    /**
     * Retorna um fragmento consoante a página selecionada
     */
    override fun getItem(position: Int): Fragment {

        //consoante a página selecionada, retornamos um fragmento especifcio
        when (position) {
            0 -> return ProductsFragment()
            //caso o utilizador seja gerente retornamos outro fragmento
            1 -> return if (admin) {
                    UsersFragment()
                }else{
                    SaleFragment()
                }
            else -> {
                return ProductsFragment()
            }
        }
    }

    /**
     * Retorna o título da página
     */
    override fun getPageTitle(position: Int): CharSequence? {
        return context.resources.getString(TAB_TITLES[position])
    }

    /**
     * Retorna o número de páginas
     */
    override fun getCount(): Int {
        return 2
    }
}