package com.example.weather_check

object ApiConfig {
    // אם אתה רץ על אמולטור ושרת localhost - השתמש ב-10.0.2.2
    // אם אתה רץ על מכשיר אמיתי - השתמש בכתובת IP האמיתית של המחשב שלך
    // לדוגמה: "http://192.168.1.100:3000" (תחליף לכתובת IP שלך)

    // למציאת כתובת IP שלך בWindows: פתח CMD והקלד "ipconfig"
    // חפש את "IPv4 Address" ברשת שלך

    private const val USE_EMULATOR = true // שנה ל-false אם אתה משתמש במכשיר אמיתי
    private const val EXTERNAL_SERVER_IP = "192.168.1.100" // הכנס כאן את כתובת ה-IP של המחשב שלך
    private const val PORT = 3000

    val BASE_URL: String = if (USE_EMULATOR) {
        "http://10.0.2.2:$PORT"
    } else {
        "http://$EXTERNAL_SERVER_IP:$PORT"
    }

    // נתיבי API
    const val PING_ENDPOINT = "/ping"
    const val REGISTER_ENDPOINT = "/register"
    const val LOGIN_ENDPOINT = "/login"
    const val WEATHER_ENDPOINT = "/weather"
    const val HISTORY_ENDPOINT = "/history"
    const val FAVORITES_ENDPOINT = "/favorites"
}



