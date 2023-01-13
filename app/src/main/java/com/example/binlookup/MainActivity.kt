package com.example.binlookup

import android.Manifest.permission.CALL_PHONE
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.os.Bundle
import android.util.JsonReader
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.binlookup.classes.Bank
import com.example.binlookup.classes.Bin
import com.example.binlookup.classes.Country
import com.example.binlookup.classes.Number
import com.example.binlookup.recycleradapters.HistoryRecyclerAdapter
import java.io.InputStream
import java.io.InputStreamReader
import java.net.URL
import java.util.*
import javax.net.ssl.HttpsURLConnection
import javax.sql.StatementEvent
import kotlin.concurrent.thread


class MainActivity : AppCompatActivity() {
    private val bin = Bin()
    private var historyList: MutableList<String> = mutableListOf()
    private val appPreferences = "mySettings"
    private val dbName = "myDB"
    private val myDB: SQLiteDatabase
        get() {
            return openOrCreateDatabase(dbName, MODE_PRIVATE, null)
        }

    // При нажатии на элемент из списка истории запросов.
    private val onItemClick: HistoryRecyclerAdapter.OnItemClickListener =
        object : HistoryRecyclerAdapter.OnItemClickListener {
            override fun onItemClick(item: String) {
                val dataField: AutoCompleteTextView =  findViewById(R.id.DataField)
                val requestButton: Button = findViewById(R.id.RequestButton)

                dataField.setText(item)
                requestButton.callOnClick()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val mSettings = getSharedPreferences(appPreferences, Context.MODE_PRIVATE)

        // Если первый запуск.
        if (!mSettings.getBoolean("firstStart", false)) {
            Databases(myDB).initialDB() // Создание базы данных.

            val editor: SharedPreferences.Editor = mSettings.edit()
            editor.putBoolean("firstStart", true)
            editor.apply()
        } else {
            historyList = Databases(myDB).getHistory()
            showHistory()
        }
    }

    // При нажатии на кнопку SEARCH.
    fun onClickSearch(view: View) {
        val dataField: AutoCompleteTextView =  findViewById(R.id.DataField)
        val num = dataField.text.toString()

        // Валидация.
        if (num == "") {
            return
        }

        addHistoryItem(num)

        // Запрос на сервер.
        val gitHubEndpoint = URL("https://lookup.binlist.net/$num")
        val connection: HttpsURLConnection = gitHubEndpoint.openConnection() as HttpsURLConnection
        connection.setRequestProperty("User-Agent", "my-rest-app-v0.1")

        thread {
            if (connection.responseCode == 200) {
                readBin(connection)
                runOnUiThread {
                    fillInValues()
                }
            } else {
                runOnUiThread {
                    Toast.makeText(
                        applicationContext,
                        "Некорректный ввод",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    // Обновить список истории и AutoComplete Adapter.
    private fun addHistoryItem(newItem: String) {
        // Добавить новый запрос в базу данных.
        Databases(myDB).setHistory(newItem)
        historyList.add(newItem)

        // Если такого запроса раньше небыло, то обновить AutoComplete и RecyclerView.
        if (!historyList.contains(newItem)) {
            showHistory()
        }
    }

    // Обновить\отобразить AutoComplete и RecyclerView.
    private fun showHistory() {
        val dataField: AutoCompleteTextView = findViewById(R.id.DataField)
        dataField.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_list_item_1, historyList)
        )

        val rvHistory: RecyclerView = findViewById(R.id.rvHistory)
        rvHistory.adapter = HistoryRecyclerAdapter(historyList, onItemClick)
        rvHistory.layoutManager = GridLayoutManager(this, 1)
    }

    // Заполнить значения на экране.
    private fun fillInValues() {
        val tvScheme: TextView = findViewById(R.id.tvScheme)
        val tvType: TextView = findViewById(R.id.tvType)
        val tvBrand: TextView = findViewById(R.id.tvBrand)
        val tvPrepaid: TextView = findViewById(R.id.tvPrepaid)
        val tvCardNumberLength: TextView = findViewById(R.id.tvCardNumberLength)
        val tvCardNumberLuhn: TextView = findViewById(R.id.tvCardNumberLuhn)
        val tvCountry: TextView = findViewById(R.id.tvCountry)
        val tvCountryLatitudeAndLongitude: TextView = findViewById(R.id.tvCountryLatitudeAndLongitude)
        val tvBankAddress: TextView = findViewById(R.id.tvBankAddress)
        val tvBankWebsite: TextView = findViewById(R.id.tvBankWebsite)
        val tvBankPhoneNumber: TextView = findViewById(R.id.tvBankPhoneNumber)

        tvScheme.text = bin.scheme
        tvType.text = bin.type
        tvBrand.text = bin.brand
        tvPrepaid.text =  if (bin.prepaid == true) "Yes" else "No"
        tvCardNumberLength.text = bin.number.length.toString()
        tvCardNumberLuhn.text = bin.number.luhn.toString()
        tvCountry.text = bin.country.emoji + " " + bin.country.name
        tvCountryLatitudeAndLongitude.text = "(latitude: ${bin.country.latitude},\n" +
                "longitude: ${bin.country.longitude})"
        tvBankAddress.text = bin.bank.name + ", " + bin.bank.city
        tvBankWebsite.text = bin.bank.url
        tvBankPhoneNumber.text = bin.bank.phone
    }

    private fun readBin(connection: HttpsURLConnection) {
        val responseBody: InputStream = connection.inputStream
        val responseBodyReader = InputStreamReader(responseBody, "UTF-8")
        val jsonReader = JsonReader(responseBodyReader)

        jsonReader.beginObject()

        while (jsonReader.hasNext()) {
            try {
                when (jsonReader.nextName()) {
                    "number" -> {
                        bin.number = readNumber(jsonReader)
                    }
                    "scheme" -> {
                        bin.scheme = jsonReader.nextString()
                    }
                    "type" -> {
                        bin.type = jsonReader.nextString()
                    }
                    "brand" -> {
                        bin.brand = jsonReader.nextString()
                    }
                    "prepaid" -> {
                        bin.prepaid = jsonReader.nextBoolean()
                    }
                    "country" -> {
                        bin.country = readCountry(jsonReader)
                    }
                    "bank" -> {
                        bin.bank = readBank(jsonReader)
                    }
                    else -> {
                        jsonReader.skipValue()
                    }
                }
            } catch (_: Exception) { }
        }

        jsonReader.close()
        connection.disconnect()
    }

    private fun readNumber(jsonReader: JsonReader): Number {
        val number = Number()
        jsonReader.beginObject()
        while (jsonReader.hasNext()) {
            when (jsonReader.nextName()) {
                "length" -> {
                    number.length = jsonReader.nextInt()
                }
                "luhn" -> {
                    number.luhn = jsonReader.nextBoolean()
                }
                else -> {
                    jsonReader.skipValue()
                }
            }
        }
        jsonReader.endObject()
        return number
    }

    private fun readCountry(jsonReader: JsonReader): Country {
        val country = Country()
        jsonReader.beginObject()
        while (jsonReader.hasNext()) {
            when (jsonReader.nextName()) {
                "numeric" -> {
                    country.numeric = jsonReader.nextString()
                }
                "alpha2" -> {
                    country.alpha2 = jsonReader.nextString()
                }
                "name" -> {
                    country.name = jsonReader.nextString()
                }
                "emoji" -> {
                    country.emoji = jsonReader.nextString()
                }
                "currency" -> {
                    country.currency = jsonReader.nextString()
                }
                "latitude" -> {
                    country.latitude = jsonReader.nextInt()
                }
                "longitude" -> {
                    country.longitude = jsonReader.nextInt()
                }
                else -> {
                    jsonReader.skipValue()
                }
            }
        }
        jsonReader.endObject()
        return country
    }

    private fun readBank(jsonReader: JsonReader): Bank {
        val bank = Bank()
        jsonReader.beginObject()
        while (jsonReader.hasNext()) {
            when (jsonReader.nextName()) {
                "name" -> {
                    bank.name = jsonReader.nextString()
                }
                "url" -> {
                    bank.url = jsonReader.nextString()
                }
                "phone" -> {
                    bank.phone = jsonReader.nextString()
                }
                "city" -> {
                    bank.city = jsonReader.nextString()
                }
                else -> {
                    jsonReader.skipValue()
                }
            }
        }
        jsonReader.endObject()
        return bank
    }

    fun onClickMap() {
        val latitude: Float? = bin.country.latitude?.toFloat()
        val longitude: Float? = bin.country.longitude?.toFloat()
        if (latitude != null && longitude != null) {
            val uri: String =
                java.lang.String.format(Locale.ENGLISH, "geo:%f,%f", latitude, longitude)

            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
            this.startActivity(intent)
        }
    }

    fun onClickSite() {
        if (bin.bank.url != null) {
            val url = "http://" + bin.bank.url
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        }
    }

    fun onClickPhoneNumber() {
        val number = bin.bank.phone
        if (number != null) {
            val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$number"))

            // Проверка на наличие прав на звонки.
            if (ContextCompat.checkSelfPermission(this, CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                startActivity(intent)
            } else {
                // Запросить права на звонки.
                ActivityCompat.requestPermissions(this, arrayOf(CALL_PHONE), 1)
            }
        }
    }
}