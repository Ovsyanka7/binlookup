package com.example.binlookup

// В случаях, когда данные невалидны, возвращается заглушка.
// Чтобы достать заглушку из ресурсов, я не нашёл способа лучше, чем
// запрашивать её при создании класса.
class Helper(private val cap: String) {

    // Возвращает заглушку, если null.
    fun validation(str: String?): String {
        if (str == "null" || str == null) {
            return cap
        } else {
            return str
        }
    }

    // Для данных типа Boolean.
    fun validation(bool: Boolean?): String {
        if (bool == null) {
            return cap
        }
        if (bool == true) {
            return "Yes"
        } else {
            return "No"
        }
    }

    /*
        Функция (или метод) используется когда две строки будут выведены вместе
        с сепаратором. В случае, если одна из строк невалидная, сепаратор не выводится.
     */
    fun validation(str1: String?, str2:String?, separator: String): String {
        // Если обе строки null.
        if ((str1 == "null" || str1 == null) && (str2 == "null" || str2 == null)) {
            return cap
        }

        // Если только первая строка null.
        if (str1 == "null" || str1 == null) {
            return str2.toString()
        }

        // Если только вторая строка null.
        if (str2 == "null" || str2 == null) {
            return str1.toString()
        }

        // Если все строки без null.
        return (str1 + separator + str2)
    }
}