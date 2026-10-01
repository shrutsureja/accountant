package com.sureja.accountant.ui

import com.sureja.accountant.data.local.CategoryEntity
import com.sureja.accountant.data.local.CategoryUsage

data class CategoryPickerSections(
    val frequent: List<CategoryEntity>,
    val remaining: List<CategoryEntity>,
)

fun categoryPickerSections(categories: List<CategoryEntity>, usage: List<CategoryUsage>): CategoryPickerSections {
    val selectable = categories.filter { it.active && it.name != "Not categorized" && it.name != "Uncategorized" }
    val byId = selectable.associateBy { it.id }
    val frequent = usage.asSequence()
        .filter { it.useCount > 0 && it.categoryId in byId }
        .sortedWith(compareByDescending<CategoryUsage> { it.useCount }
            .thenBy(String.CASE_INSENSITIVE_ORDER) { byId.getValue(it.categoryId).name }
            .thenBy { it.categoryId })
        .map { byId.getValue(it.categoryId) }
        .distinctBy { it.id }
        .take(5)
        .toList()
    val frequentIds = frequent.mapTo(mutableSetOf()) { it.id }
    val remaining = selectable.filterNot { it.id in frequentIds }
        .sortedWith(compareBy<CategoryEntity, String>(String.CASE_INSENSITIVE_ORDER) { it.name }.thenBy { it.id })
    return CategoryPickerSections(frequent, remaining)
}
