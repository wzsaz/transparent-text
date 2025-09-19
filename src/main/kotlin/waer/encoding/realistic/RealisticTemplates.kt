package waer.encoding.realistic

/**
 * Represents a realistic sentence template with proper grammar structure
 */
data class SentenceTemplate(
    val id: Int,
    val pattern: String,  // e.g. "I really enjoyed {activity} last {timeperiod} because it was {adjective}."
    val slotTypes: List<String>,  // e.g. ["activity", "timeperiod", "adjective"]
    val category: String = "general"  // topic category for coherence
)

/**
 * Represents a paragraph template for multi-sentence coherence
 */
data class ParagraphTemplate(
    val id: Int,
    val sentenceTemplates: List<Int>,  // IDs of sentence templates to use
    val topic: String,  // e.g. "travel", "food", "work"
    val style: TextStyle
)

enum class TextStyle {
    BLOG_POST,
    EMAIL,
    STORY,
    NEWS_ARTICLE,
    SOCIAL_MEDIA,
    PERSONAL_DIARY
}

/**
 * Word categories with realistic, contextually appropriate words
 */
object WordCategories {

    val activities = listOf(
        "hiking", "reading", "cooking", "traveling", "shopping", "exercising", "painting", "writing",
        "swimming", "cycling", "gardening", "photography", "dancing", "singing", "studying", "working",
        "volunteering", "exploring", "relaxing", "meditating", "jogging", "skiing", "surfing", "climbing",
        "fishing", "camping", "sailing", "driving", "flying", "visiting", "meeting", "chatting",
        "learning", "teaching", "creating", "building", "designing", "planning", "organizing", "cleaning"
    )

    val timePeriods = listOf(
        "weekend", "week", "month", "summer", "winter", "spring", "fall", "year", "holiday",
        "vacation", "evening", "morning", "afternoon", "day", "Tuesday", "Wednesday", "Thursday",
        "Friday", "Saturday", "Sunday", "Monday", "Christmas", "birthday", "anniversary"
    )

    val positiveAdjectives = listOf(
        "amazing", "wonderful", "fantastic", "incredible", "beautiful", "peaceful", "relaxing",
        "exciting", "inspiring", "rewarding", "satisfying", "enjoyable", "memorable", "perfect",
        "lovely", "charming", "delightful", "pleasant", "refreshing", "invigorating", "stunning",
        "breathtaking", "remarkable", "extraordinary", "magnificent", "brilliant", "excellent"
    )

    val places = listOf(
        "park", "beach", "mountain", "cafe", "restaurant", "museum", "library", "mall", "store",
        "office", "gym", "school", "university", "hospital", "airport", "station", "hotel",
        "home", "garden", "forest", "lake", "river", "city", "town", "village", "neighborhood"
    )

    val people = listOf(
        "friend", "colleague", "neighbor", "family", "sister", "brother", "mother", "father",
        "cousin", "aunt", "uncle", "teacher", "doctor", "manager", "teammate", "classmate",
        "partner", "spouse", "roommate", "boss", "client", "student", "instructor", "mentor"
    )

    val foods = listOf(
        "pizza", "pasta", "salad", "soup", "sandwich", "burger", "sushi", "coffee", "tea",
        "cake", "cookies", "bread", "cheese", "fruit", "vegetables", "chicken", "fish",
        "rice", "noodles", "ice cream", "chocolate", "wine", "beer", "juice", "smoothie"
    )

    val emotions = listOf(
        "happy", "excited", "grateful", "proud", "satisfied", "content", "cheerful", "optimistic",
        "relaxed", "peaceful", "energetic", "motivated", "inspired", "confident", "comfortable",
        "surprised", "curious", "thoughtful", "reflective", "hopeful", "blessed", "fortunate"
    )

    val objects = listOf(
        "book", "phone", "computer", "car", "bike", "camera", "watch", "bag", "shoes", "jacket",
        "gift", "ticket", "letter", "photo", "painting", "flower", "plant", "tool", "instrument",
        "game", "movie", "song", "recipe", "map", "key", "document", "certificate", "medal"
    )
}

/**
 * Collection of realistic sentence templates
 */
object SentenceTemplates {

    val templates = listOf(
        // Personal experiences
        SentenceTemplate(1, "I really enjoyed {activity} last {timeperiod} because it was {adjective}.",
            listOf("activity", "timeperiod", "adjective"), "personal"),

        SentenceTemplate(2, "Yesterday I went to the {place} and had a {adjective} time with my {person}.",
            listOf("place", "adjective", "person"), "personal"),

        SentenceTemplate(3, "My {person} recommended this {place} and I think it's absolutely {adjective}.",
            listOf("person", "place", "adjective"), "recommendation"),

        SentenceTemplate(4, "The {food} at the new {place} was surprisingly {adjective}.",
            listOf("food", "place", "adjective"), "food"),

        SentenceTemplate(5, "I've been {activity} more often lately and feeling really {emotion}.",
            listOf("activity", "emotion"), "personal"),

        // Observations and descriptions
        SentenceTemplate(6, "The weather this {timeperiod} has been perfect for {activity}.",
            listOf("timeperiod", "activity"), "weather"),

        SentenceTemplate(7, "I noticed that the {place} is always busy during {timeperiod}.",
            listOf("place", "timeperiod"), "observation"),

        SentenceTemplate(8, "There's something {adjective} about {activity} in the early morning.",
            listOf("adjective", "activity"), "reflection"),

        SentenceTemplate(9, "My new {object} has made {activity} so much more {adjective}.",
            listOf("object", "activity", "adjective"), "personal"),

        SentenceTemplate(10, "I can't believe how {adjective} the {place} looked during {timeperiod}.",
            listOf("adjective", "place", "timeperiod"), "description"),

        // Social interactions
        SentenceTemplate(11, "Had lunch with my {person} today and we talked about {activity}.",
            listOf("person", "activity"), "social"),

        SentenceTemplate(12, "My {person} suggested we try {activity} together next {timeperiod}.",
            listOf("person", "activity", "timeperiod"), "social"),

        SentenceTemplate(13, "Everyone at the {place} was so {adjective} and welcoming.",
            listOf("place", "adjective"), "social"),

        // Future plans and thoughts
        SentenceTemplate(14, "I'm planning to do more {activity} this {timeperiod}.",
            listOf("activity", "timeperiod"), "plans"),

        SentenceTemplate(15, "Next {timeperiod} I want to visit that {adjective} {place} again.",
            listOf("timeperiod", "adjective", "place"), "plans"),

        SentenceTemplate(16, "I should really spend more time {activity} instead of working.",
            listOf("activity"), "reflection"),

        // Memories and nostalgia
        SentenceTemplate(17, "I remember when {activity} used to be my favorite way to spend {timeperiod}.",
            listOf("activity", "timeperiod"), "memory"),

        SentenceTemplate(18, "That {place} always reminds me of {activity} with my {person}.",
            listOf("place", "activity", "person"), "memory"),

        // Current activities
        SentenceTemplate(19, "Currently {activity} and enjoying every minute of it.",
            listOf("activity"), "current"),

        SentenceTemplate(20, "Just finished {activity} and feeling {emotion} about the results.",
            listOf("activity", "emotion"), "current")
    )
}

/**
 * Paragraph templates for coherent multi-sentence text
 */
object ParagraphTemplates {

    val templates = listOf(
        // Personal experience paragraph
        ParagraphTemplate(1, listOf(1, 5, 14), "personal_experience", TextStyle.BLOG_POST),

        // Place review paragraph
        ParagraphTemplate(2, listOf(3, 4, 13), "place_review", TextStyle.BLOG_POST),

        // Activity reflection paragraph
        ParagraphTemplate(3, listOf(8, 16, 19), "activity_reflection", TextStyle.PERSONAL_DIARY),

        // Social update paragraph
        ParagraphTemplate(4, listOf(11, 12, 20), "social_update", TextStyle.SOCIAL_MEDIA),

        // Memory and nostalgia paragraph
        ParagraphTemplate(5, listOf(17, 18, 15), "nostalgia", TextStyle.PERSONAL_DIARY)
    )
}
