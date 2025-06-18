package common

interface LocalizedString : CharSequence {

    val default: String
    val en: String? get() = null // English (USA, UK, global)
    val es: String? get() = null // Spanish (Spain, Latin America)
    val pt: String? get() = null // Portuguese (Brazil, Portugal)
    val fr: String? get() = null // French (France, Canada, parts of Africa)
    val de: String? get() = null // German (Germany, Austria, Switzerland)
    val ru: String? get() = null // Russian (Russia, Eastern Europe, Central Asia)
    val it: String? get() = null // Italian (Italy)
    val pl: String? get() = null // Polish (Poland)
    val uk: String? get() = null // Ukrainian (Ukraine)
    val nl: String? get() = null // Dutch (Netherlands, Belgium)
    val ro: String? get() = null // Romanian (Romania, Moldova)
    val tr: String? get() = null // Turkish (Turkey)
    val ar: String? get() = null // Arabic (Middle East, North Africa)
    val fa: String? get() = null // Persian / Farsi (Iran)
    val hi: String? get() = null // Hindi (India)
    val ur: String? get() = null // Urdu (Pakistan, India)
    val bn: String? get() = null // Bengali (Bangladesh, India)
    val ta: String? get() = null // Tamil (India, Sri Lanka, Singapore)
    val te: String? get() = null // Telugu (India)
    val mr: String? get() = null // Marathi (India)
    val gu: String? get() = null // Gujarati (India)
    val pa: String? get() = null // Punjabi (India, Pakistan)
    val zh: String? get() = null // Chinese (Mandarin, China, Taiwan, Singapore)
    val hu: String? get() = null // Hungarian
    val sk: String? get() = null // Slovak
    val cs: String? get() = null // Czech
    val ja: String? get() = null // Japanese (Japan)
    val ko: String? get() = null // Korean (South Korea)
    val vi: String? get() = null // Vietnamese (Vietnam)
    val th: String? get() = null // Thai (Thailand)
    val id: String? get() = null // Indonesian (Indonesia)
    val sw: String? get() = null // Swahili (East Africa)
    val jv: String? get() = null // Javanese (Indonesia)

    override val length: Int
        get() = default.length

    override fun get(index: Int) = default[index]

    override fun subSequence(startIndex: Int, endIndex: Int) = default.subSequence(startIndex, endIndex)

}

/**
 * Creates a [LocalizedString] instance with the provided default and optional translations.
 */
fun t(
    default: String,
    en: String? = null,
    es: String? = null,
    pt: String? = null,
    fr: String? = null,
    de: String? = null,
    ru: String? = null,
    it: String? = null,
    pl: String? = null,
    uk: String? = null,
    nl: String? = null,
    ro: String? = null,
    tr: String? = null,
    ar: String? = null,
    fa: String? = null,
    hi: String? = null,
    ur: String? = null,
    bn: String? = null,
    ta: String? = null,
    te: String? = null,
    mr: String? = null,
    gu: String? = null,
    pa: String? = null,
    zh: String? = null,
    hu: String? = null,
    sk: String? = null,
    cs: String? = null,
    ja: String? = null,
    ko: String? = null,
    vi: String? = null,
    th: String? = null,
    id: String? = null,
    sw: String? = null,
    jv: String? = null
) = object : LocalizedString {
    override val default: String = default
    override val en: String? = en
    override val es: String? = es
    override val pt: String? = pt
    override val fr: String? = fr
    override val de: String? = de
    override val ru: String? = ru
    override val it: String? = it
    override val pl: String? = pl
    override val uk: String? = uk
    override val nl: String? = nl
    override val ro: String? = ro
    override val tr: String? = tr
    override val ar: String? = ar
    override val fa: String? = fa
    override val hi: String? = hi
    override val ur: String? = ur
    override val bn: String? = bn
    override val ta: String? = ta
    override val te: String? = te
    override val mr: String? = mr
    override val gu: String? = gu
    override val pa: String? = pa
    override val zh: String? = zh
    override val hu: String? = hu
    override val sk: String? = sk
    override val cs: String? = cs
    override val ja: String? = ja
    override val ko: String? = ko
    override val vi: String? = vi
    override val th: String? = th
    override val id: String? = id
    override val sw: String? = sw
    override val jv: String? = jv
}
