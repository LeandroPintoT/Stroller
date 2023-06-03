package cl.panmu.stroller.ui.main

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter


internal class PagerAdapter (fm: FragmentManager, lifecycle: Lifecycle) : FragmentStateAdapter(fm, lifecycle) {
    // Array of images
    // Adding images from drawable folder
    private val fragments = ArrayList<Fragment>()
    private val fragmentsTitles = ArrayList<String>()

    // This Method returns the size of the Array
    override fun getItemCount(): Int {
        return fragments.size
    }

    override fun createFragment(position: Int): Fragment {
        return fragments[position]
    }

    fun addFragment(frag: Fragment, titulo: String) {
        fragments.add(frag)
        fragmentsTitles.add(titulo)
    }

    fun getFragmentTitle(pos: Int): String {
        return fragmentsTitles[pos]
    }
}