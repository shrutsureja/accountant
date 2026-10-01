package com.sureja.accountant.ui

import com.sureja.accountant.data.local.CategoryEntity
import com.sureja.accountant.data.local.CategoryUsage
import org.junit.Assert.assertEquals
import org.junit.Test

class CategoryPickerOrderTest {
    private fun category(id: String, name: String) = CategoryEntity(id, name, createdAt = "", updatedAt = "")

    @Test fun recentFrequencyFirstThenStableNameOrder() {
        val categories = listOf(category("other", "Other"), category("food", "Food"), category("bills", "Bills"), category("travel", "Travel"))
        val sections = categoryPickerSections(categories, listOf(CategoryUsage("food", 8), CategoryUsage("other", 3)))
        assertEquals(listOf("Food", "Other"), sections.frequent.map { it.name })
        assertEquals(listOf("Bills", "Travel"), sections.remaining.map { it.name })
    }

    @Test fun uncategorizedIsNeverSelectableAndOtherRemains() {
        val categories = listOf(category("uncategorized", "Not categorized"), category("other", "Other"), category("food", "Food"))
        val sections = categoryPickerSections(categories, listOf(CategoryUsage("uncategorized", 100), CategoryUsage("other", 2)))
        assertEquals(listOf("Other"), sections.frequent.map { it.name })
        assertEquals(listOf("Food"), sections.remaining.map { it.name })
    }

    @Test fun frequentListIsLimitedToFive() {
        val categories = (1..7).map { category("c$it", "Category $it") }
        val sections = categoryPickerSections(categories, (1..7).map { CategoryUsage("c$it", it) })
        assertEquals(listOf("c7", "c6", "c5", "c4", "c3"), sections.frequent.map { it.id })
        assertEquals(listOf("c1", "c2"), sections.remaining.map { it.id })
    }
}
