# 🧠 FSRS Scheduler (Version 6) – Kotlin Implementation

This repository contains a **Kotlin implementation** of the [Free Spaced Repetition Scheduler (FSRS)](https://github.com/open-spaced-repetition), version **6**. FSRS is a modern algorithm for spaced repetition, optimized using machine learning to improve long-term retention and scheduling efficiency.

> 🔬 FSRS v6 is the latest official version of the algorithm, offering better accuracy and flexibility compared to older algorithms like SM-2.

---

## ✅ Features

- 📌 Based on the **official FSRS v6 specification**
- 🔄 Supports all review responses: `Again`, `Hard`, `Good`, `Easy`
- 🧮 Returns updated **stability**, **difficulty**, and **interval**
- 🧱 Written in idiomatic **Kotlin**, easy to integrate into Android or JVM projects
- 🕒 Uses `LocalDateTime` for precise due date handling
- 🧪 Suitable for experimentation or direct production use

---

## 📦 Use Cases

This FSRS implementation can be used in:

- Flashcard apps (Anki-like)
- Educational software for medical, language, and test prep
- Any Kotlin-based spaced repetition project

---

## 🚀 Example Usage

```kotlin
//create instance, use deck default retention(0.9) and default params
//FSRS-6 sepecific params = [0.212, 1.2931, 2.3065, 8.2956, 6.4133, 0.8334, 3.0194, 0.001, 1.8722, 0.1666, 0.796, 1.4835, 0.0614, 0.2629, 1.6483, 0.6014, 1.8729, 0.5425, 0.0912, 0.0658, 0.1542]
val fsrs = FSRS(deck.retention, deck.params)

//calculate a list contains stability, difficulty, interval, durationMillis and text to be shown for each button
//see models.kt file
val gradeList = fsrs.calculate(flashCard)

//to update card, just add interval to current time
fun addMillisToNow(millis: Long): LocalDateTime {
        val nowInstant = Instant.now()
        val newInstant = nowInstant.plusMillis(millis)
        return LocalDateTime.ofInstant(newInstant, ZoneId.systemDefault())
    } 
```
## 📚 References
- [FSRS ALgorithm](https://github.com/open-spaced-repetition/fsrs4anki)
- [FSRS v6 Documentation & Research Paper](https://github.com/open-spaced-repetition/fsrs4anki/wiki/Research-resources)

## 🛠 License
This implementation is provided under the MIT License. Attribution to the original FSRS author is appreciated.

## 🙌 Contributing
Feel free to fork the repo and submit pull requests if you’d like to add enhancements or bug fixes. Issues and suggestions are always welcome.
