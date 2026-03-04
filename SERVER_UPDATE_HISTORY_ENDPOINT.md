# עדכון חשוב: מחיקת היסטוריה לפי Timestamp

## הבעיה שתוקנה
כשמחקנו עיר בהיסטוריה, **כל הרשומות עם אותו שם עיר** נמחקו (למשל כל חיפושי "Tel Aviv").
המשתמש רצה למחוק רק את החיפוש הספציפי שלחץ עליו.

## הפתרון
במקום למחוק לפי שם העיר, עכשיו מוחקים לפי **timestamp** - מזהה ייחודי לכל חיפוש.

---

## שינויים שבוצעו באפליקציה

### 1. תיקון DataMappers.kt
**לפני:**
```kotlin
fun HistoryItem.toWeatherResponse(): WeatherResponse {
    return WeatherResponse(
        // ...
        timestamp = System.currentTimeMillis()  // ❌ תמיד אותו timestamp!
    )
}
```

**אחרי:**
```kotlin
fun HistoryItem.toWeatherResponse(): WeatherResponse {
    return WeatherResponse(
        // ...
        timestamp = this.timestamp  // ✅ השתמש ב-timestamp המקורי מהשרת
    )
}
```

### 2. עדכון ApiConfig.kt
```kotlin
const val HISTORY_BY_TIMESTAMP_ENDPOINT_TEMPLATE = "/api/history/%s"
// השרת יקבל: /api/history/1709380800
```

### 3. עדכון ApiHelper.kt
```kotlin
fun removeHistoryItemWithAuth(token: String, timestamp: Long): Response {
    val endpoint = ApiConfig.HISTORY_BY_TIMESTAMP_ENDPOINT_TEMPLATE.format(timestamp.toString())
    return client.newCall(buildDeleteRequest(endpoint, token)).execute()
}
```

### 4. עדכון NetworkRepository.kt
```kotlin
fun removeHistoryItem(
    token: String,
    timestamp: Long,  // ✅ כעת מקבל timestamp במקום city name
    onSuccess: (HistoryResponse) -> Unit,
    onError: (String, Int?) -> Unit
)
```

### 5. מחיקה מקומית מיידית + סנכרון עם שרת
עכשיו בשני המסכים (HomeActivity ו-HistoryActivity):

```kotlin
private fun deleteHistoryItem(item: WeatherResponse) {
    // 1. מחיקה מקומית מיידית - העיר נעלמת מיד מהמסך
    historyItems.removeAll { it.timestamp == item.timestamp }
    listAdapter.submitItems(historyItems)
    
    // 2. קריאה לשרת במקביל
    NetworkRepository.removeHistoryItem(
        token = token,
        timestamp = item.timestamp,  // שליחת timestamp ספציפי
        onSuccess = { response ->
            // סנכרון עם השרת
            historyItems = response.history.map { it.toWeatherResponse() }.toMutableList()
            listAdapter.submitItems(historyItems)
        },
        onError = { error, statusCode ->
            // במקרה של שגיאה - טוען מחדש מהשרת כדי לשחזר מצב
            loadHistoryData()
        }
    )
}
```

**יתרונות:**
- ✅ המחיקה מיידית - המשתמש רואה שהפריט נעלם מיד
- ✅ אם השרת נכשל - הנתונים חוזרים (error recovery)
- ✅ אם השרת מצליח - מסנכרן עם המצב האמיתי

---

## מה צריך לעדכן בשרת Node.js

### שינוי ה-Endpoint הקיים:

**לפני (לפי שם עיר):**
```javascript
// ❌ זה מוחק את כל הרשומות עם אותו שם עיר
app.delete('/api/history/:city', authenticateToken, async (req, res) => {
    const cityToDelete = decodeURIComponent(req.params.city);
    user.history = user.history.filter(item => 
        item.city.toLowerCase() !== cityToDelete.toLowerCase()
    );
    // ...
});
```

**אחרי (לפי timestamp):**
```javascript
// ✅ זה מוחק רק את הרשומה הספציפית
app.delete('/api/history/:timestamp', authenticateToken, async (req, res) => {
    try {
        const userId = req.user.id;
        const timestampToDelete = parseInt(req.params.timestamp);
        
        if (isNaN(timestampToDelete)) {
            return res.status(400).json({ error: 'Invalid timestamp' });
        }
        
        // מצא את המשתמש
        const user = await User.findById(userId);
        if (!user) {
            return res.status(404).json({ error: 'User not found' });
        }

        // מחק רק את הרשומה עם ה-timestamp הספציפי
        const initialLength = user.history.length;
        user.history = user.history.filter(item => item.timestamp !== timestampToDelete);
        
        // בדוק אם משהו נמחק
        if (user.history.length === initialLength) {
            return res.status(404).json({ error: 'History item not found' });
        }

        await user.save();

        // החזר את ההיסטוריה המעודכנת
        res.status(200).json({
            history: user.history
        });
    } catch (error) {
        console.error('Error deleting history item:', error);
        res.status(500).json({ error: 'Failed to delete history item' });
    }
});
```

### נקודות חשובות:

1. **Timestamp הוא מספר שלם (Long/Integer)**
   - צריך לעשות `parseInt()` על הפרמטר
   - לוודא שזה מספר תקין

2. **השוואה מדויקת**
   - `item.timestamp !== timestampToDelete` (לא case-insensitive כמו עם strings)

3. **Validation**
   - אם ה-timestamp לא תקין → 400 Bad Request
   - אם הפריט לא נמצא → 404 Not Found

4. **Response Format** (אותו דבר):
   ```json
   {
     "history": [
       {
         "city": "Tel Aviv",
         "country": "IL",
         "temp": 22.5,
         "feelsLike": 21.8,
         "humidity": 65,
         "windSpeed": 3.5,
         "description": "clear sky",
         "icon": "01d",
         "timestamp": 1709380800,
         "searchedAt": "2026-03-03T09:30:00.000Z"
       }
     ]
   }
   ```

### דוגמת Request:
```
DELETE http://localhost:3000/api/history/1709380800
Headers:
  Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

---

## בדיקה

1. הפעל את השרת עם השינויים החדשים
2. באפליקציה: חפש אותה עיר מספר פעמים (למשל "Tel Aviv" 3 פעמים)
3. היכנס ל"היסטוריה" - תראה 3 רשומות של Tel Aviv
4. לחץ על כפתור הפח ליד הרשומה **האמצעית**
5. ✅ רק הרשומה האמצעית צריכה להיעלם
6. ✅ שתי הרשומות האחרות צריכות להישאר

---

## סיכום

| לפני | אחרי |
|------|------|
| מחק לפי שם עיר | מחק לפי timestamp |
| כל "Tel Aviv" נמחקות | רק החיפוש הספציפי נמחק |
| `/api/history/Tel%20Aviv` | `/api/history/1709380800` |
| מסנכרן רק עם שרת | מחיקה מקומית מיידית + סנכרון |

תאריך: 4 במרץ 2026

