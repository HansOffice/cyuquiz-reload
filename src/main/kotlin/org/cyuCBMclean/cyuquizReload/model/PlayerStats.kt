package org.cyuCBMclean.cyuquizReload.model

class PlayerStats(
    answered: Int = 0,
    correct: Int = 0,
    wrong: Int = 0,
    currentStreak: Int = 0,
    highestStreak: Int = 0
) {
    @Volatile
    var answered: Int = answered
        private set
    @Volatile
    var correct: Int = correct
        private set
    @Volatile
    var wrong: Int = wrong
        private set
    @Volatile
    var currentStreak: Int = currentStreak
        private set
    @Volatile
    var highestStreak: Int = highestStreak
        private set

    @Synchronized
    fun incrementCorrect() {
        answered++
        correct++
        currentStreak++
        if (currentStreak > highestStreak) {
            highestStreak = currentStreak
        }
    }

    @Synchronized
    fun incrementWrong() {
        answered++
        wrong++
        currentStreak = 0
    }
}