package com.ipt.simpleproductspos.ui.fragment

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.viewpager.widget.PagerAdapter
import com.ipt.simpleproductspos.R
import java.util.*

/**
 * Código adaptado de: https://www.youtube.com/watch?v=mvlCKRpKVZs&ab_channel=AppClasses
 */
class ViewPagerAdapter(val context: Context, private val imageList: List<Int>) : PagerAdapter() {
    /**
     * retorna a quantidade de imagens na lista
     */
    override fun getCount(): Int {
        return imageList.size
    }

    /**
     * retorna true caso a view e o object tenham a mesma referência
     */
    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view === `object` as RelativeLayout
    }

    /**
     * Retorna a vista de um item do viewpager
     */
    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val mLayoutInflater =
            context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        val itemView: View = mLayoutInflater.inflate(R.layout.slider_item, container, false)


        val imageView: ImageView = itemView.findViewById<View>(R.id.idIVImage) as ImageView


        imageView.setImageResource(imageList[position])


        Objects.requireNonNull(container).addView(itemView)

        return itemView
    }

    /**
     * remove um objeto da view
     */
    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as RelativeLayout)
    }
}