package com.example.binlookup

import android.Manifest.permission.CALL_PHONE
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.binlookup.classes.Bin
import com.example.binlookup.recycleradapters.HistoryRecyclerAdapter
import java.net.URL
import java.util.*
import javax.net.ssl.HttpsURLConnection
import kotlin.concurrent.thread


class MainActivity : AppCompatActivity() {
    private var bin = Bin() // Объект для хранения данных с сервера.
    private var historyList: MutableList<String> = mutableListOf() // История поиска.
    private val appPreferences = "mySettings"
    private val dbName = "myDB"
    // Я перенёс логику работы с БД в отдельный класс (Databases). И использую этот геттер, чтобы
    // база данных работала. Можно обойтись и без геттера, но тогда эта строка будет дублироваться.
    private val myDB: SQLiteDatabase
        get() {
            return openOrCreateDatabase(dbName, MODE_PRIVATE, null)
        }

    // При нажатии на элемент из списка истории запросов.
    private val onItemClick: HistoryRecyclerAdapter.OnItemClickListener =
        object : HistoryRecyclerAdapter.OnItemClickListener {
            override fun onItemClick(item: String) {
                val dataField: AutoCompleteTextView =  findViewById(R.id.DataField)

                dataField.setText(item)
                dataField.dismissDropDown() // Скрыть выпадающий список (чтобы не мешал).
                onClickSearch()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bSearch: Button = findViewById(R.id.bSearch)
        val tvBankAddress: TextView = findViewById(R.id.tvBankAddress)
        val tvBankPhoneNumber: TextView = findViewById(R.id.tvBankPhoneNumber)
        val tvBankWebsite: TextView = findViewById(R.id.tvBankWebsite)
        val mSettings = getSharedPreferences(appPreferences, Context.MODE_PRIVATE)

        // Если первый запуск.
        if (!mSettings.getBoolean("firstStart", false)) {
            // Создание базы данных.
            Databases(myDB).initialDB()

            val editor: SharedPreferences.Editor = mSettings.edit()
            editor.putBoolean("firstStart", true)
            editor.apply()
        } else {
            historyList = Databases(myDB).getHistory()
            showHistory()
        }

        // Перенёс логику по разным функциям, чтобы код было легче читать.
        bSearch.setOnClickListener {onClickSearch()}
        tvBankAddress.setOnClickListener {onClickAddress()}
        tvBankPhoneNumber.setOnClickListener {onClickPhoneNumber()}
        tvBankWebsite.setOnClickListener {onClickSite()}
    }

    // При нажатии на кнопку SEARCH.
    fun onClickSearch() {
        val dataField: AutoCompleteTextView = findViewById(R.id.DataField)
        val num = dataField.text.toString()

        // Валидация.
        if (num == "") {
            return
        }

        addHistoryItem(num)
        showHistory()

        // Запрос на сервер.
        val gitHubEndpoint = URL("https://lookup.binlist.net/$num")
        val connection: HttpsURLConnection = gitHubEndpoint.openConnection() as HttpsURLConnection

        thread {
            if (connection.responseCode == 200) {
                // Логика дешфровки данных с сервера перенесена в другой класс (JsonReader).
                bin = JsonReader().readBin(connection)
                runOnUiThread {
                    // Заполнение данных.
                    fillInValues()
                }
            } else {
                runOnUiThread {
                    Toast.makeText(
                        applicationContext,
                        "Некорректный ввод",
                        Toast.LENGTH_SHORT
                    ).show()

                    // Удаляем данные.
                    bin = Bin()
                    fillInValues()
                }
            }
        }
    }

    // Добавить новый элемент истории в базу данных.
    private fun addHistoryItem(newItem: String) {
        Databases(myDB).setHistory(newItem)
        historyList.add(0, newItem)
    }

    // Обновить\отобразить AutoComplete и RecyclerView.
    private fun showHistory() {
        val dataField: AutoCompleteTextView = findViewById(R.id.DataField)
        dataField.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_list_item_1, historyList.distinct())
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

        // Я перенёс логику валидации данных в другой класс (helper),
        // И отправляю в этот класс заглушку на случай, если данные будут невалидными,
        // Так как я не нашёл способа достать заглушку из ресурсов в другом классе.
        val helper = Helper(getString(R.string.cap))

        // Схема.
        tvScheme.text = helper.validation(bin.scheme)
        // Тип.
        tvType.text = helper.validation(bin.type)
        // Бренд.
        tvBrand.text = helper.validation(bin.brand)
        // Предоплата.
        tvPrepaid.text = helper.validation(bin.prepaid)
        // Номер карты.
        tvCardNumberLength.text = helper.validation(bin.number.length.toString())
        tvCardNumberLuhn.text = helper.validation(bin.number.luhn)
        // Страна.
        tvCountry.text = helper.validation(bin.country.emoji, bin.country.name, " ")
        // Координаты.
        val latitude = helper.validation(bin.country.latitude.toString())
        val longitude = helper.validation(bin.country.longitude.toString())
        tvCountryLatitudeAndLongitude.text =
            getString(R.string.country_latitude_and_longitude, latitude, longitude)
        // Адрес.
        tvBankAddress.text = helper.validation(bin.bank.name, bin.bank.city, ", ")
        // Сайт.
        tvBankWebsite.text = helper.validation(bin.bank.url)
        // Номер телефона.
        tvBankPhoneNumber.text = helper.validation(bin.bank.phone)
    }

    // При нажатии на адрес банка (город, название банка).
    // (Иногда переход по ссылке срабатывает, даже если вместо текста заглушка)
    // (Это связано с тем, что, иногда, координаты есть, а адреса нет).
    private fun onClickAddress() {
        val latitude: Float? = bin.country.latitude?.toFloat()
        val longitude: Float? = bin.country.longitude?.toFloat()

        if (latitude != null && longitude != null) {
            val uri: String =
                java.lang.String.format(Locale.ENGLISH, "geo:%f,%f", latitude, longitude)

            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
            this.startActivity(intent)
        }
    }

    // При нажатии на Web-сайт.
    private fun onClickSite() {
        if (bin.bank.url != null) {
            val url = "http://" + bin.bank.url
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        }
    }

    // При нажатии на номер телефона.
    private fun onClickPhoneNumber() {
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