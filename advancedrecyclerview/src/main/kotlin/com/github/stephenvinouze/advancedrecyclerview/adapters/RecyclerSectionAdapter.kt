package com.github.stephenvinouze.advancedrecyclerview.adapters

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import com.github.stephenvinouze.advancedrecyclerview.views.BaseViewHolder
import java.util.*

/**
 * Created by Stephen Vinouze on 09/11/2015.
 */
abstract class RecyclerSectionAdapter<K, T>(context: Context, section: (T) -> K): RecyclerAdapter<T>(context) {

    var section: (T) -> K
    var sectionItems: LinkedHashMap<K, MutableList<T>> = linkedMapOf()
        private set

    private val SECTION_TYPE = 0

    init {
        this.section = section
    }

    abstract fun onCreateSectionItemView(parent: ViewGroup, viewType: Int): View
    abstract fun onBindSectionItemView(itemView: View, section: Int)

    override var items: MutableList<T> = arrayListOf()
        get() = field
        set(value) {
            buildSections(value, section)

            var reorderedItems: MutableList<T> = arrayListOf()
            for (items in sectionItems.values) {
                reorderedItems.addAll(items)
            }

            field = reorderedItems
            notifyDataSetChanged()
        }

    override fun handleClick(viewHolder: BaseViewHolder, clickPosition: (BaseViewHolder) -> Int) {
        super.handleClick(viewHolder, { relativePosition(it.layoutPosition) })
    }

    override fun addItems(items: List<T>, position: Int) {
        this.items.addAll(relativePosition(position), items)
        buildSections(items, section)
    }

    override fun addItem(item: T, position: Int) {
        super.addItem(item, relativePosition(position))
        buildSections(items, section)
    }

    override fun moveItem(from: Int, to: Int) {
        super.moveItem(relativePosition(from), relativePosition(to))
        buildSections(items, section)
    }

    override fun removeItem(position: Int) {
        super.removeItem(relativePosition(position))
        buildSections(items, section)
    }

    override fun toggleItemView(position: Int) {
        super.toggleItemView(position)
        notifyItemChanged(absolutePosition(position))
    }

    override fun getItemViewType(position: Int): Int {
        return if (isSectionAt(position)) SECTION_TYPE else super.getItemViewType(relativePosition(position)) + 1
    }

    override fun getItemId(position: Int): Long {
        return if (isSectionAt(position)) Long.MAX_VALUE - sectionPosition(position) else super.getItemId(relativePosition(position))
    }

    override fun getItemCount(): Int {
        return super.getItemCount() + numberOfSections()
    }

    final override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        if (viewType == SECTION_TYPE) {
            return BaseViewHolder(onCreateSectionItemView(parent, viewType))
        } else {
            return super.onCreateViewHolder(parent, viewType - 1)
        }
    }

    final override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        if (isSectionAt(position)) {
            onBindSectionItemView(holder.view, sectionPosition(position))
        } else {
            super.onBindViewHolder(holder, relativePosition(position))
        }
    }

    fun numberOfSections(): Int {
        return sectionItems.size
    }

    fun numberOfItemsInSection(section: Int): Int {
        return sectionItems[sectionAt(section)]?.size ?: 0
    }

    fun sectionAt(position: Int): Any? {
        return allSections()[position]
    }

    fun allSections(): List<K> {
        return sectionItems.keys.toList()
    }

    fun buildSections(items: List<T>, section: (T) -> K) {
        sectionItems = linkedMapOf()

        for (item in items) {
            val itemSection = section(item)
            val itemsInSection = sectionItems[itemSection] ?: arrayListOf()

            itemsInSection.add(item)

            sectionItems.put(itemSection, itemsInSection)
        }
    }

    /**
     * Check that the given position in the list matches with a section position
     * @param position: The absolute position in the list
     * @return True if this is a section
     */
    fun isSectionAt(position: Int): Boolean {
        var absoluteSectionPosition = 0
        for (section in 0..numberOfSections() - 1) {
            if (position == absoluteSectionPosition) {
                return true
            } else if (position < absoluteSectionPosition) {
                return false
            }

            absoluteSectionPosition += numberOfItemsInSection(section) + 1
        }
        return false
    }

    /**
     * Compute the relative section position in the list depending on the number of items in each section
     * @param position: The absolute position in the list
     * @return The relative section position of the given position
     */
    fun sectionPosition(position: Int): Int {
        var sectionPosition = 0
        var absoluteSectionPosition = 0
        for (section in 0..numberOfSections() - 1) {
            absoluteSectionPosition += numberOfItemsInSection(section)
            if (position <= absoluteSectionPosition) {
                return sectionPosition
            }

            sectionPosition++
            absoluteSectionPosition++
        }
        return sectionPosition
    }

    /**
     * Compute the relative position in the list that omits the sections
     * @param position: The absolute position in the list
     * @return The relative position without sections or NO_POSITION if matches section position
     */
    fun relativePosition(position: Int): Int {
        if (isSectionAt(position)) {
            return RecyclerView.NO_POSITION
        }

        var relativePosition = position
        for (absolutePosition in 0..position) {
            if (isSectionAt(absolutePosition)) {
                relativePosition--
            }
        }
        return relativePosition
    }

    /**
     * Compute the absolute position in the list that includes the sections
     * @param position: The relative position in the list
     * @return The absolute position with sections
     */
    fun absolutePosition(position: Int): Int {
        var offset = 0
        for (relativePosition in 0..position) {
            if (isSectionAt(relativePosition + offset)) {
                offset++
            }
        }
        return position + offset
    }
}