package com.example.binlookup

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import androidx.appcompat.app.AppCompatActivity

class Databases(private val myDB: SQLiteDatabase) : AppCompatActivity() {
    private val tableHistory = "tableHistory"
    
    fun initialDB() {
        myDB.execSQL(
            "CREATE TABLE IF NOT EXISTS $tableHistory " +
                    "(_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "number VARCHAR(10))"
        )   
        myDB.close()
    }

    // Возвращает поисковую историю.
    fun getHistory(): MutableList<String> {
        val list = mutableListOf<String>()
        val myCursor: Cursor = myDB.rawQuery(
            "SELECT number FROM $tableHistory",
            null
        )
        while (myCursor.moveToNext()) {
            val number = myCursor.getString(0)
            list.add(number)
        }
        myCursor.close()
        myDB.close()
        return list
    }

    // Добавляет поисковый запрос к истории.
    fun setHistory(num: String) {
        val row = ContentValues()
        row.put("number", num)
        myDB.insert(tableHistory, null, row)
        myDB.close()
    }
}