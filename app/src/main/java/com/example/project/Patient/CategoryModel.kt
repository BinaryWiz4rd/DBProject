package com.example.project.Patient

/**
 * Data class representing a category.
 *
 * This class holds information about a specific category, including its unique identifier,
 * name, and a URL or path to its associated picture.
 *
 * @property Id The unique integer identifier for the category. Defaults to 0.
 * @property Name The name of the category. Defaults to an empty string.
 * @property Picture The URL or path to the category's picture. Defaults to an empty string.
 */
data class CategoryModel(
    val Id:Int=0,
    val Name:String="",
    val Picture:String=""
)