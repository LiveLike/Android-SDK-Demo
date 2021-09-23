package com.livelike.demo.ui.main

import android.content.Context
import android.content.SharedPreferences
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.livelike.demo.DemoMainActivity
import com.livelike.demo.R

private val TAB_TITLES = arrayOf(
        R.string.tab_text_1,
        R.string.tab_text_2,
        R.string.tab_text_3
)

/**
 * A [FragmentPagerAdapter] that returns a fragment corresponding to
 * one of the sections/tabs/pages.
 */
class SectionsPagerAdapter(private val context: Context, fm: FragmentManager)
    : FragmentPagerAdapter(fm) {

    val sharedPreferences : SharedPreferences
    init {
        sharedPreferences = context.getSharedPreferences(DemoMainActivity.ID_SHARED_PREFS, Context.MODE_PRIVATE)
    }
    override fun getItem(position: Int): BaseFragment {
        // getItem is called to instantiate the fragment for the given page.
        // Return a PlaceholderFragment (defined as a static inner class below).
        if(position == 0) {
            return sharedPreferences.getString(DemoMainActivity.PUBLIC_CHAT_ID_KEY,"")?.let { ChatFragment.newInstance(it, true) }!!
        } else if(position == 1) {
            return sharedPreferences.getString(DemoMainActivity.INFLUENCER_CHAT_ID_KEY,"")?.let { ChatFragment.newInstance(it, false) }!!
        } else if(position == 2) {
            return WidgetTimeLineFragment.newInstance("39a245dd-2a83-42e8-83fb-442d7adad0f6")
        }
        return ChatFragment.newInstance("39a245dd-2a83-42e8-83fb-442d7adad0f6", true)

    }

    override fun getPageTitle(position: Int): CharSequence? {
        return context.resources.getString(TAB_TITLES[position])
    }

    override fun getCount(): Int {
        // Show 2 total pages.
        return 3
    }
}